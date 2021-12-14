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
abstract class ExchangeTaskMapper {

  suspend fun getExchangeTaskForStep(context: ExchangeContext): ExchangeTask {
    @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
    return when (context.step.stepCase) {
      StepCase.ENCRYPT_STEP -> context.encrypt()
      StepCase.REENCRYPT_STEP -> context.reEncrypt()
      StepCase.DECRYPT_STEP -> context.decrypt()
      StepCase.INPUT_STEP -> context.input()
      StepCase.COPY_FROM_PREVIOUS_EXCHANGE_STEP -> context.copyFromPreviousExchange()
      StepCase.GENERATE_COMMUTATIVE_DETERMINISTIC_KEY_STEP -> context.generateSymmetricKey()
      StepCase.GENERATE_SERIALIZED_RLWE_KEYS_STEP -> context.generateSerializedRlweKeys()
      StepCase.GENERATE_CERTIFICATE_STEP -> context.generateExchangeCertificate()
      StepCase.GENERATE_LOOKUP_KEYS -> context.generateLookupKeys()
      StepCase.INTERSECT_AND_VALIDATE_STEP -> context.intersectAndValidate()
      StepCase.EXECUTE_PRIVATE_MEMBERSHIP_QUERIES_STEP -> context.executePrivateMembershipQueries()
      StepCase.BUILD_PRIVATE_MEMBERSHIP_QUERIES_STEP -> context.buildPrivateMembershipQueries()
      StepCase.DECRYPT_PRIVATE_MEMBERSHIP_QUERY_RESULTS_STEP -> context.decryptMembershipResults()
      StepCase.COPY_FROM_SHARED_STORAGE_STEP -> context.copyFromSharedStorage()
      StepCase.COPY_TO_SHARED_STORAGE_STEP -> context.copyToSharedStorage()
      else -> throw IllegalArgumentException("Unsupported step type: ${context.step.stepCase}")
    }
  }

  /** Returns the task that encrypts. */
  abstract fun ExchangeContext.encrypt(): ExchangeTask

  /** Returns the task that decrypts. */
  abstract fun ExchangeContext.decrypt(): ExchangeTask

  /** Returns the task that re-encrypts. */
  abstract fun ExchangeContext.reEncrypt(): ExchangeTask

  /** Returns the task that generates an encryption key. */
  abstract fun ExchangeContext.generateEncryptionKey(): ExchangeTask

  /** Returns the task that builds private membership queries. */
  abstract suspend fun ExchangeContext.buildPrivateMembershipQueries(): ExchangeTask

  /** Returns the task that executes the private membership queries. */
  abstract suspend fun ExchangeContext.executePrivateMembershipQueries(): ExchangeTask

  /** Returns the task that decrypts the private membership queries. */
  abstract suspend fun ExchangeContext.decryptMembershipResults(): ExchangeTask

  /** Returns the task that generates a symmetric key. */
  abstract fun ExchangeContext.generateSymmetricKey(): ExchangeTask

  /** Returns the task that generates serialized rlwe keys. */
  abstract fun ExchangeContext.generateSerializedRlweKeys(): ExchangeTask

  /** Returns the task that generates a certificate. */
  abstract fun ExchangeContext.generateExchangeCertificate(): ExchangeTask

  /** Returns the task that generates lookup keys. */
  abstract fun ExchangeContext.generateLookupKeys(): ExchangeTask

  /** Returns the task that validates the step. */
  abstract fun ExchangeContext.intersectAndValidate(): ExchangeTask

  /** Returns the task that gets the input. */
  abstract suspend fun ExchangeContext.input(): ExchangeTask

  /** Returns the task that copies from previous [Exchange]. */
  abstract suspend fun ExchangeContext.copyFromPreviousExchange(): ExchangeTask

  /** Returns the task that copies to a shared storage. */
  abstract suspend fun ExchangeContext.copyToSharedStorage(): ExchangeTask

  /** Returns the task that copies from the shared storage. */
  abstract suspend fun ExchangeContext.copyFromSharedStorage(): ExchangeTask
}
