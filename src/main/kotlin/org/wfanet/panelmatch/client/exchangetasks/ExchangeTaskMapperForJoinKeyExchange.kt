// Copyright 2021 The Cross-Media Measurement Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.wfanet.panelmatch.client.exchangetasks

import com.google.protobuf.ByteString
import org.apache.beam.sdk.Pipeline
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow.Step.StepCase
import org.wfanet.measurement.common.throttler.Throttler
import org.wfanet.measurement.common.toLocalDate
import org.wfanet.panelmatch.client.common.ExchangeContext
import org.wfanet.panelmatch.client.privatemembership.CreateQueriesParameters
import org.wfanet.panelmatch.client.privatemembership.EvaluateQueriesParameters
import org.wfanet.panelmatch.client.privatemembership.PrivateMembershipCryptor
import org.wfanet.panelmatch.client.privatemembership.QueryEvaluator
import org.wfanet.panelmatch.client.privatemembership.QueryPreparer
import org.wfanet.panelmatch.client.privatemembership.QueryResultsDecryptor
import org.wfanet.panelmatch.client.storage.PrivateStorageSelector
import org.wfanet.panelmatch.client.storage.SharedStorageSelector
import org.wfanet.panelmatch.common.ShardedFileName
import org.wfanet.panelmatch.common.certificates.CertificateManager
import org.wfanet.panelmatch.common.crypto.DeterministicCommutativeCipher

/** Maps join key exchange steps to exchange tasks */
abstract class ExchangeTaskMapperForJoinKeyExchange : ExchangeTaskMapper {
  abstract val deterministicCommutativeCryptor: DeterministicCommutativeCipher
  abstract val getPrivateMembershipCryptor: (ByteString) -> PrivateMembershipCryptor
  abstract val queryPreparer: QueryPreparer
  abstract val queryResultsDecryptor: QueryResultsDecryptor
  abstract val getQueryResultsEvaluator: (ByteString) -> QueryEvaluator
  abstract val privateStorageSelector: PrivateStorageSelector
  abstract val sharedStorageSelector: SharedStorageSelector
  abstract val certificateManager: CertificateManager
  abstract val inputTaskThrottler: Throttler

  abstract fun newPipeline(): Pipeline

  override suspend fun getExchangeTaskForStep(context: ExchangeContext): ExchangeTask {
    return context.getExchangeTask()
  }

  private suspend fun ExchangeContext.getExchangeTask(): ExchangeTask {
    @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
    return when (step.stepCase) {
      StepCase.ENCRYPT_STEP -> CryptorExchangeTask.forEncryption(deterministicCommutativeCryptor)
      StepCase.REENCRYPT_STEP ->
        CryptorExchangeTask.forReEncryption(deterministicCommutativeCryptor)
      StepCase.DECRYPT_STEP -> CryptorExchangeTask.forDecryption(deterministicCommutativeCryptor)
      StepCase.GENERATE_LOOKUP_KEYS_STEP -> JoinKeyHashingExchangeTask.forHashing(queryPreparer)
      StepCase.INPUT_STEP -> getInputStepTask()
      StepCase.INTERSECT_AND_VALIDATE_STEP -> getIntersectAndValidateStepTask()
      StepCase.GENERATE_COMMUTATIVE_DETERMINISTIC_KEY_STEP ->
        GenerateSymmetricKeyTask(generateKey = deterministicCommutativeCryptor::generateKey)
      StepCase.GENERATE_SERIALIZED_RLWE_KEYS_STEP -> getGenerateSerializedRlweKeysStepTask()
      StepCase.GENERATE_CERTIFICATE_STEP ->
        GenerateExchangeCertificateTask(certificateManager, exchangeDateKey)
      StepCase.EXECUTE_PRIVATE_MEMBERSHIP_QUERIES_STEP -> getExecutePrivateMembershipQueriesTask()
      StepCase.BUILD_PRIVATE_MEMBERSHIP_QUERIES_STEP -> getBuildPrivateMembershipQueriesTask()
      StepCase.DECRYPT_PRIVATE_MEMBERSHIP_QUERY_RESULTS_STEP -> getDecryptMembershipResultsTask()
      StepCase.COPY_FROM_SHARED_STORAGE_STEP -> getCopyFromSharedStorageTask()
      StepCase.COPY_TO_SHARED_STORAGE_STEP -> getCopyToSharedStorageTask()
      StepCase.COPY_FROM_PREVIOUS_EXCHANGE_STEP -> getCopyFromPreviousExchangeTask()
      else -> throw IllegalArgumentException("Unsupported step type: ${step.stepCase}")
    }
  }

  private suspend fun ExchangeContext.getInputStepTask(): ExchangeTask {
    check(step.stepCase == StepCase.INPUT_STEP)
    require(step.inputLabelsMap.isEmpty())
    val blobKey = step.outputLabelsMap.values.single()
    return InputTask(
      storage = privateStorageSelector.getStorageClient(exchangeDateKey),
      throttler = inputTaskThrottler,
      blobKey = blobKey,
    )
  }

  private fun ExchangeContext.getIntersectAndValidateStepTask(): ExchangeTask {
    check(step.stepCase == StepCase.INTERSECT_AND_VALIDATE_STEP)

    val maxSize = step.intersectAndValidateStep.maxSize
    val maximumNewItemsAllowed = step.intersectAndValidateStep.maximumNewItemsAllowed

    return IntersectValidateTask(
      maxSize = maxSize,
      maximumNewItemsAllowed = maximumNewItemsAllowed,
      isFirstExchange = date == workflow.firstExchangeDate.toLocalDate()
    )
  }

  private fun ExchangeContext.getGenerateSerializedRlweKeysStepTask(): ExchangeTask {
    check(step.stepCase == StepCase.GENERATE_SERIALIZED_RLWE_KEYS_STEP)
    val privateMembershipCryptor =
      getPrivateMembershipCryptor(step.generateSerializedRlweKeysStep.serializedParameters)
    return GenerateAsymmetricKeysTask(generateKeys = privateMembershipCryptor::generateKeys)
  }

  private suspend fun ExchangeContext.getBuildPrivateMembershipQueriesTask(): ExchangeTask {
    check(step.stepCase == StepCase.BUILD_PRIVATE_MEMBERSHIP_QUERIES_STEP)
    val stepDetails = step.buildPrivateMembershipQueriesStep
    val privateMembershipCryptor = getPrivateMembershipCryptor(stepDetails.serializedParameters)
    val outputManifests =
      mapOf(
        "encrypted-queries" to stepDetails.encryptedQueryBundleFileCount,
        "query-to-ids-map" to stepDetails.queryIdToIdsFileCount,
      )

    val parameters =
      CreateQueriesParameters(
        numShards = stepDetails.numShards,
        numBucketsPerShard = stepDetails.numBucketsPerShard,
        maxQueriesPerShard = stepDetails.numQueriesPerShard,
        padQueries = stepDetails.addPaddingQueries,
      )

    return apacheBeamTaskFor(outputManifests) {
      buildPrivateMembershipQueries(parameters, privateMembershipCryptor)
    }
  }

  private suspend fun ExchangeContext.getDecryptMembershipResultsTask(): ExchangeTask {
    check(step.stepCase == StepCase.DECRYPT_PRIVATE_MEMBERSHIP_QUERY_RESULTS_STEP)
    val stepDetails = step.decryptPrivateMembershipQueryResultsStep

    val outputManifests = mapOf("decrypted-event-data" to stepDetails.decryptEventDataSetFileCount)

    return apacheBeamTaskFor(outputManifests) {
      decryptPrivateMembershipResults(stepDetails.serializedParameters, queryResultsDecryptor)
    }
  }

  private suspend fun ExchangeContext.getExecutePrivateMembershipQueriesTask(): ExchangeTask {
    check(step.stepCase == StepCase.EXECUTE_PRIVATE_MEMBERSHIP_QUERIES_STEP)
    val stepDetails = step.executePrivateMembershipQueriesStep

    val parameters =
      EvaluateQueriesParameters(
        numShards = stepDetails.numShards,
        numBucketsPerShard = stepDetails.numBucketsPerShard,
        maxQueriesPerShard = stepDetails.maxQueriesPerShard
      )

    val queryResultsEvaluator = getQueryResultsEvaluator(stepDetails.serializedParameters)

    val outputManifests = mapOf("encrypted-results" to stepDetails.encryptedQueryResultFileCount)

    return apacheBeamTaskFor(outputManifests) {
      executePrivateMembershipQueries(parameters, queryResultsEvaluator)
    }
  }

  private suspend fun ExchangeContext.getCopyFromSharedStorageTask(): ExchangeTask {
    check(step.stepCase == StepCase.COPY_FROM_SHARED_STORAGE_STEP)
    val source = sharedStorageSelector.getSharedStorage(workflow.exchangeIdentifiers.storage, this)
    val destination = privateStorageSelector.getStorageClient(exchangeDateKey)
    return CopyFromSharedStorageTask(
      source,
      destination,
      step.copyFromSharedStorageStep.copyOptions,
      step.inputLabelsMap.values.single(),
      step.outputLabelsMap.values.single()
    )
  }

  private suspend fun ExchangeContext.getCopyToSharedStorageTask(): ExchangeTask {
    check(step.stepCase == StepCase.COPY_TO_SHARED_STORAGE_STEP)
    val source = privateStorageSelector.getStorageClient(exchangeDateKey)
    val destination =
      sharedStorageSelector.getSharedStorage(workflow.exchangeIdentifiers.storage, this)
    return CopyToSharedStorageTask(
      source,
      destination,
      step.copyToSharedStorageStep.copyOptions,
      step.inputLabelsMap.values.single(),
      step.outputLabelsMap.values.single()
    )
  }

  private suspend fun ExchangeContext.getCopyFromPreviousExchangeTask(): ExchangeTask {
    check(step.stepCase == StepCase.COPY_FROM_PREVIOUS_EXCHANGE_STEP)

    val previousBlobKey = step.inputLabelsMap.getValue("input")

    if (exchangeDateKey.date == workflow.firstExchangeDate.toLocalDate()) {
      return InputTask(
        previousBlobKey,
        inputTaskThrottler,
        privateStorageSelector.getStorageClient(exchangeDateKey)
      )
    }

    return CopyFromPreviousExchangeTask(
      privateStorageSelector,
      workflow.repetitionSchedule,
      exchangeDateKey,
      previousBlobKey
    )
  }

  private suspend fun ExchangeContext.apacheBeamTaskFor(
    outputManifests: Map<String, Int>,
    execute: suspend ApacheBeamContext.() -> Unit
  ): ApacheBeamTask {
    return ApacheBeamTask(
      newPipeline(),
      privateStorageSelector.getStorageFactory(exchangeDateKey),
      step.inputLabelsMap,
      outputManifests.mapValues { (k, v) -> ShardedFileName(step.outputLabelsMap.getValue(k), v) },
      execute
    )
  }
}
