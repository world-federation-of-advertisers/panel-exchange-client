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

import java.time.Clock
import java.time.Duration
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow.Step.StepCase
import org.wfanet.measurement.common.throttler.MinimumIntervalThrottler
import org.wfanet.measurement.common.throttler.Throttler
import org.wfanet.panelmatch.protocol.common.DeterministicCommutativeCipher

/** Maps join key exchange steps to exchange tasks */
class ExchangeTaskMapperForJoinKeyExchange(
  private val deterministicCommutativeCryptor: DeterministicCommutativeCipher,
  private val throttler: Throttler =
    MinimumIntervalThrottler(Clock.systemUTC(), Duration.ofMillis(100))
) : ExchangeTaskMapper {

  override suspend fun getExchangeTaskForStep(step: ExchangeWorkflow.Step): ExchangeTaskDetails {
    @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
    return when (step.stepCase) {
      StepCase.ENCRYPT_STEP ->
        ExchangeTaskDetails(
          CryptorExchangeTask.forEncryption(deterministicCommutativeCryptor),
          readsInput = true,
          writesOutput = true,
          requiredStorageClient = ExchangeTaskStorageType.NONE
        )
      StepCase.REENCRYPT_STEP ->
        ExchangeTaskDetails(
          CryptorExchangeTask.forReEncryption(deterministicCommutativeCryptor),
          readsInput = true,
          writesOutput = true,
          requiredStorageClient = ExchangeTaskStorageType.NONE
        )
      StepCase.DECRYPT_STEP ->
        ExchangeTaskDetails(
          CryptorExchangeTask.forDecryption(deterministicCommutativeCryptor),
          readsInput = true,
          writesOutput = true,
          requiredStorageClient = ExchangeTaskStorageType.NONE
        )
      StepCase.INPUT_STEP ->
        ExchangeTaskDetails(
          InputTask(throttler = throttler),
          readsInput = false,
          writesOutput = false,
          requiredStorageClient = ExchangeTaskStorageType.PRIVATE
        )
      StepCase.INTERSECT_AND_VALIDATE_STEP ->
        ExchangeTaskDetails(
          IntersectValidateTask(
            maxSize = step.intersectAndValidateStep.maxSize,
            minimumOverlap = step.intersectAndValidateStep.minimumOverlap
          ),
          readsInput = true,
          writesOutput = true,
          requiredStorageClient = ExchangeTaskStorageType.NONE
        )
      StepCase.EXECUTE_PRIVATE_MEMBERSHIP_QUERIES_STEP -> TODO()
      StepCase.BUILD_PRIVATE_MEMBERSHIP_QUERIES_STEP -> TODO()
      StepCase.DECRYPT_PRIVATE_MEMBERSHIP_QUERY_RESULTS_STEP -> TODO()
      StepCase.COPY_FROM_SHARED_STORAGE_STEP -> TODO()
      StepCase.COPY_TO_SHARED_STORAGE_STEP -> TODO()
      else -> error("Unsupported step type")
    }
  }
}
