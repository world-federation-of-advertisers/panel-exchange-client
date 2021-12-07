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
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow.Step.StepCase
import org.wfanet.measurement.common.throttler.Throttler
import org.wfanet.panelmatch.client.common.ExchangeContext
import org.wfanet.panelmatch.client.privatemembership.PrivateMembershipCryptor
import org.wfanet.panelmatch.client.privatemembership.QueryEvaluator
import org.wfanet.panelmatch.client.privatemembership.QueryResultsDecryptor
import org.wfanet.panelmatch.client.storage.PrivateStorageSelector
import org.wfanet.panelmatch.client.storage.SharedStorageSelector
import org.wfanet.panelmatch.common.certificates.CertificateManager
import org.wfanet.panelmatch.common.crypto.DeterministicCommutativeCipher
import src.main.kotlin.org.wfanet.panelmatch.client.exchangetasks.CommutativeEncryptionTasks
import src.main.kotlin.org.wfanet.panelmatch.client.exchangetasks.SharedStorageTasks

/** Maps join key exchange steps to exchange tasks */
abstract class ExchangeTaskMapper(
  private val basicTasks: BasicTasks,
  private val commutativeEncryptionTasks: CommutativeEncryptionTasks,
  private val mapReduceTasks: MapReduceTasks,
  private val generateKeysTasks: GenerateKeysTasks,
  private val privateStorageTasks: PrivateStorageTasks,
  private val sharedStorageTasks: SharedStorageTasks
) :
  BasicTasks by basicTasks,
  CommutativeEncryptionTasks by commutativeEncryptionTasks,
  SharedStorageTasks by sharedStorageTasks,
  PrivateStorageTasks by privateStorageTasks,
  MapReduceTasks by mapReduceTasks,
  GenerateKeysTasks by generateKeysTasks {

  abstract val deterministicCommutativeCryptor: DeterministicCommutativeCipher
  abstract val getPrivateMembershipCryptor: (ByteString) -> PrivateMembershipCryptor
  abstract val queryResultsDecryptor: QueryResultsDecryptor
  abstract val getQueryResultsEvaluator: (ByteString) -> QueryEvaluator
  abstract val privateStorageSelector: PrivateStorageSelector
  abstract val sharedStorageSelector: SharedStorageSelector
  abstract val certificateManager: CertificateManager
  abstract val inputTaskThrottler: Throttler

  suspend fun getExchangeTaskForStep(context: ExchangeContext): ExchangeTask {
    return context.getExchangeTask()
  }

  private suspend fun ExchangeContext.getExchangeTask(): ExchangeTask {
    @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
    return when (step.stepCase) {
      StepCase.ENCRYPT_STEP -> CryptorExchangeTask.forEncryption(deterministicCommutativeCryptor)
      StepCase.REENCRYPT_STEP ->
        CryptorExchangeTask.forReEncryption(deterministicCommutativeCryptor)
      StepCase.DECRYPT_STEP -> CryptorExchangeTask.forDecryption(deterministicCommutativeCryptor)
      StepCase.INPUT_STEP -> getInputStepTask()
      StepCase.GENERATE_LOOKUP_KEYS -> getGenerateLookupKeysTask()
      StepCase.INTERSECT_AND_VALIDATE_STEP -> getIntersectAndValidateStepTask()
      StepCase.GENERATE_COMMUTATIVE_DETERMINISTIC_KEY_STEP ->
        getGenerateSymmetricKeyTask(deterministicCommutativeCryptor)
      StepCase.GENERATE_SERIALIZED_RLWE_KEYS_STEP ->
        getGenerateSerializedRlweKeysStepTask(getPrivateMembershipCryptor)
      StepCase.GENERATE_CERTIFICATE_STEP ->
        GenerateExchangeCertificateTask(certificateManager, exchangeDateKey)
      StepCase.EXECUTE_PRIVATE_MEMBERSHIP_QUERIES_STEP ->
        getExecutePrivateMembershipQueriesTask(getQueryResultsEvaluator)
      StepCase.BUILD_PRIVATE_MEMBERSHIP_QUERIES_STEP ->
        getBuildPrivateMembershipQueriesTask(getPrivateMembershipCryptor)
      StepCase.DECRYPT_PRIVATE_MEMBERSHIP_QUERY_RESULTS_STEP ->
        getDecryptMembershipResultsTask(queryResultsDecryptor)
      StepCase.COPY_FROM_SHARED_STORAGE_STEP -> buildCopyFromSharedStorageTask()
      StepCase.COPY_TO_SHARED_STORAGE_STEP -> buildCopyToSharedStorageTask()
      StepCase.COPY_FROM_PREVIOUS_EXCHANGE_STEP -> getCopyFromPreviousExchangeTask()
      else -> throw IllegalArgumentException("Unsupported step type: ${step.stepCase}")
    }
  }
}
