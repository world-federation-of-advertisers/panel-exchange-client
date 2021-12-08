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
) {

  suspend fun getExchangeTaskForStep(context: ExchangeContext): ExchangeTask {
    @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
    return when (context.step.stepCase) {
      StepCase.ENCRYPT_STEP -> commutativeEncryptionTasks.encrypt()
      StepCase.REENCRYPT_STEP -> commutativeEncryptionTasks.reEncrypt()
      StepCase.DECRYPT_STEP -> commutativeEncryptionTasks.decrypt()
      StepCase.INPUT_STEP -> privateStorageTasks.input(context)
      StepCase.COPY_FROM_PREVIOUS_EXCHANGE_STEP ->
        privateStorageTasks.copyFromPreviousExchange(context)
      StepCase.INTERSECT_AND_VALIDATE_STEP -> validationTasks.intersectAndValidate(context)
      StepCase.GENERATE_COMMUTATIVE_DETERMINISTIC_KEY_STEP ->
        generateKeysTasks.generateSymmetricKey()
      StepCase.GENERATE_SERIALIZED_RLWE_KEYS_STEP ->
        generateKeysTasks.generateSerializedRlweKeys(context)
      StepCase.GENERATE_CERTIFICATE_STEP -> generateKeysTasks.generateExchangeCertificate(context)
      StepCase.GENERATE_LOOKUP_KEYS -> generateKeysTasks.generateLookupKeys()
      StepCase.EXECUTE_PRIVATE_MEMBERSHIP_QUERIES_STEP ->
        mapReduceTasks.executePrivateMembershipQueries(context)
      StepCase.BUILD_PRIVATE_MEMBERSHIP_QUERIES_STEP ->
        mapReduceTasks.buildPrivateMembershipQueries(context)
      StepCase.DECRYPT_PRIVATE_MEMBERSHIP_QUERY_RESULTS_STEP ->
        mapReduceTasks.decryptMembershipResults(context)
      StepCase.COPY_FROM_SHARED_STORAGE_STEP -> sharedStorageTasks.copyFromSharedStorage(context)
      StepCase.COPY_TO_SHARED_STORAGE_STEP -> sharedStorageTasks.copyToSharedStorage(context)
      else -> throw IllegalArgumentException("Unsupported step type: ${context.step.stepCase}")
    }
  }
}
