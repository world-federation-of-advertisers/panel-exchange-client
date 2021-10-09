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
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow.Step.StepCase
import org.wfanet.measurement.common.throttler.Throttler
import org.wfanet.panelmatch.client.privatemembership.CreateQueriesParameters
import org.wfanet.panelmatch.client.privatemembership.PrivateMembershipCryptor
import org.wfanet.panelmatch.client.privatemembership.QueryResultsDecryptor
import org.wfanet.panelmatch.client.storage.StorageFactory
import org.wfanet.panelmatch.common.compression.CompressorFactory
import org.wfanet.panelmatch.common.crypto.DeterministicCommutativeCipher

/** Maps join key exchange steps to exchange tasks */
// TODO make this class abstract and move the constructor params to lazy props
class ExchangeTaskMapperForJoinKeyExchange(
  private val compressorFactory: CompressorFactory,
  private val getDeterministicCommutativeCryptor: () -> DeterministicCommutativeCipher,
  private val getPrivateMembershipCryptor: (ByteString) -> PrivateMembershipCryptor,
  private val getQueryResultsDecryptor: () -> QueryResultsDecryptor,
  private val privateStorage: StorageFactory,
  private val inputTaskThrottler: Throttler
) : ExchangeTaskMapper {

  override suspend fun getExchangeTaskForStep(step: ExchangeWorkflow.Step): ExchangeTask {
    // TODO move each step into a separate function
    @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
    return when (step.stepCase) {
      StepCase.ENCRYPT_STEP ->
        CryptorExchangeTask.forEncryption(getDeterministicCommutativeCryptor())
      StepCase.REENCRYPT_STEP ->
        CryptorExchangeTask.forReEncryption(getDeterministicCommutativeCryptor())
      StepCase.DECRYPT_STEP ->
        CryptorExchangeTask.forDecryption(getDeterministicCommutativeCryptor())
      StepCase.INPUT_STEP -> {
        require(step.inputLabelsMap.isEmpty())
        val blobKey = step.outputLabelsMap.values.single()
        InputTask(
          storage = privateStorage.build(),
          blobKey = blobKey,
          throttler = inputTaskThrottler
        )
      }
      StepCase.INTERSECT_AND_VALIDATE_STEP ->
        IntersectValidateTask(
          maxSize = step.intersectAndValidateStep.maxSize,
          minimumOverlap = step.intersectAndValidateStep.minimumOverlap
        )
      StepCase.GENERATE_COMMUTATIVE_DETERMINISTIC_KEY_STEP ->
        GenerateSymmetricKeyTask(generateKey = getDeterministicCommutativeCryptor()::generateKey)
      StepCase.GENERATE_SERIALIZED_RLWE_KEYS_STEP -> {
        val privateMembershipCryptor =
          getPrivateMembershipCryptor(step.generateSerializedRlweKeysStep.serializedParameters)
        GenerateAsymmetricKeysTask(generateKeys = privateMembershipCryptor::generateKeys)
      }
      StepCase.GENERATE_CERTIFICATE_STEP -> TODO()
      StepCase.EXECUTE_PRIVATE_MEMBERSHIP_QUERIES_STEP -> TODO()
      StepCase.BUILD_PRIVATE_MEMBERSHIP_QUERIES_STEP -> {
        val privateMembershipCryptor =
          getPrivateMembershipCryptor(step.buildPrivateMembershipQueriesStep.serializedParameters)
        val outputs =
          BuildPrivateMembershipQueriesTask.Outputs(
            encryptedQueriesFileCount =
              step.buildPrivateMembershipQueriesStep.encryptedQueryBundleFileCount,
            encryptedQueriesFileName = step.outputLabelsMap.getValue("encrypted-queries"),
            queryIdAndPanelistKeyFileCount =
              step.buildPrivateMembershipQueriesStep.queryIdAndPanelistKeyFileCount,
            queryIdAndPanelistKeyFileName = step.outputLabelsMap.getValue("query-decryption-keys"),
          )
        BuildPrivateMembershipQueriesTask(
          outputs = outputs,
          parameters =
            CreateQueriesParameters(
              numShards = step.buildPrivateMembershipQueriesStep.numShards,
              numBucketsPerShard = step.buildPrivateMembershipQueriesStep.numBucketsPerShard,
              maxQueriesPerShard = step.buildPrivateMembershipQueriesStep.numQueriesPerShard,
              // TODO get `padQueries` from new field at step.buildPrivateMembershipQueriesStep
              padQueries = true,
            ),
          privateMembershipCryptor = privateMembershipCryptor,
          storageFactory = privateStorage
        )
      }
      StepCase.DECRYPT_PRIVATE_MEMBERSHIP_QUERY_RESULTS_STEP -> {
        val outputs =
          DecryptPrivateMembershipResultsTask.Outputs(
            decryptedEventDataSetFileCount =
              step.decryptPrivateMembershipQueryResultsStep.decryptEventDataSetFileCount,
            decryptedEventDataSetFileName = step.outputLabelsMap.getValue("decrypted-event-data"),
          )
        DecryptPrivateMembershipResultsTask(
          serializedParameters = step.decryptPrivateMembershipQueryResultsStep.serializedParameters,
          queryResultsDecryptor = getQueryResultsDecryptor(),
          compressorFactory = compressorFactory,
          outputs = outputs,
          storageFactory = privateStorage
        )
      }
      StepCase.COPY_FROM_SHARED_STORAGE_STEP -> TODO()
      StepCase.COPY_TO_SHARED_STORAGE_STEP -> TODO()
      else -> error("Unsupported step type")
    }
  }
}
