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

import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow.Step.StepCase
import org.wfanet.panelmatch.client.common.ExchangeContext

/** Maps join key exchange steps to exchange tasks */
class ExchangeTaskMapper(
  private val validationTasks: ValidationTasks,
  private val commutativeEncryptionTasks: CommutativeEncryptionTasks,
  private val mapReduceTasks: MapReduceTasks,
  private val generateKeysTasks: GenerateKeysTasks,
  private val privateStorageTasks: PrivateStorageTasks,
  private val sharedStorageTasks: SharedStorageTasks
) :
  ValidationTasks by validationTasks,
  CommutativeEncryptionTasks by commutativeEncryptionTasks,
  SharedStorageTasks by sharedStorageTasks,
  PrivateStorageTasks by privateStorageTasks,
  MapReduceTasks by mapReduceTasks,
  GenerateKeysTasks by generateKeysTasks {

  suspend fun getExchangeTaskForStep(context: ExchangeContext): ExchangeTask {
    @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
    return when (context.step.stepCase) {
      StepCase.ENCRYPT_STEP -> context.buildEncryptTask()
      StepCase.REENCRYPT_STEP -> context.buildReEncryptTask()
      StepCase.DECRYPT_STEP -> context.buildDecryptTask()
      StepCase.INPUT_STEP -> context.getInputStepTask()
      StepCase.GENERATE_LOOKUP_KEYS -> getGenerateLookupKeysTask()
      StepCase.INTERSECT_AND_VALIDATE_STEP -> context.getIntersectAndValidateStepTask()
      StepCase.GENERATE_COMMUTATIVE_DETERMINISTIC_KEY_STEP -> getGenerateSymmetricKeyTask()
      StepCase.GENERATE_SERIALIZED_RLWE_KEYS_STEP -> context.getGenerateSerializedRlweKeysStepTask()
      StepCase.GENERATE_CERTIFICATE_STEP -> context.getGenerateExchangeCertificateTask()
      StepCase.EXECUTE_PRIVATE_MEMBERSHIP_QUERIES_STEP ->
        context.getExecutePrivateMembershipQueriesTask()
      StepCase.BUILD_PRIVATE_MEMBERSHIP_QUERIES_STEP ->
        context.getBuildPrivateMembershipQueriesTask()
      StepCase.DECRYPT_PRIVATE_MEMBERSHIP_QUERY_RESULTS_STEP ->
        context.getDecryptMembershipResultsTask()
      StepCase.COPY_FROM_SHARED_STORAGE_STEP -> context.buildCopyFromSharedStorageTask()
      StepCase.COPY_TO_SHARED_STORAGE_STEP -> context.buildCopyToSharedStorageTask()
      StepCase.COPY_FROM_PREVIOUS_EXCHANGE_STEP -> context.getCopyFromPreviousExchangeTask()
      else -> throw IllegalArgumentException("Unsupported step type: ${context.step.stepCase}")
    }
  }
}
