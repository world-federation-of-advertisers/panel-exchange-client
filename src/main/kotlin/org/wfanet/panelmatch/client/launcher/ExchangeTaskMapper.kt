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

package org.wfanet.panelmatch.client.launcher

import com.google.protobuf.ByteString
import java.time.Duration
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttempt
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.panelmatch.client.exchangetasks.CryptorExchangeTask
import org.wfanet.panelmatch.client.exchangetasks.ExchangeTask
import org.wfanet.panelmatch.client.exchangetasks.InputTask
import org.wfanet.panelmatch.client.logger.addToTaskLog
import org.wfanet.panelmatch.client.logger.getAndClearTaskLog
import org.wfanet.panelmatch.client.logger.loggerFor
import org.wfanet.panelmatch.client.storage.Storage
import org.wfanet.panelmatch.client.storage.getAllInputForStep
import org.wfanet.panelmatch.client.storage.writeAllOutputForStep
import org.wfanet.panelmatch.protocol.common.Cryptor
import org.wfanet.panelmatch.protocol.common.JniDeterministicCommutativeCryptor

/**
 * Maps ExchangeWorkflow.Step to respective tasks. Retrieves necessary inputs. Executes step. Stores
 * outputs.
 *
 * @param ExchangeWorkflow.Step to execute.
 * @param input inputs needed by all [task]s.
 * @param storage the Storage class to store the intermediary steps
 * @return mapped output.
 */
class ExchangeTaskMapper(
  private val deterministicCommutativeCryptor: Cryptor = JniDeterministicCommutativeCryptor(),
  private val timeoutDuration: Duration = Duration.ofDays(1),
  private val retryDuration: Duration = Duration.ofMinutes(1),
  private val preferredSharedStorage: Storage,
  private val preferredPrivateStorage: Storage
) {

  private suspend fun getExchangeTaskForStep(step: ExchangeWorkflow.Step): ExchangeTask {
    return when (step.getStepCase()) {
      ExchangeWorkflow.Step.StepCase.ENCRYPT_STEP ->
        CryptorExchangeTask.forEncryption(deterministicCommutativeCryptor)
      ExchangeWorkflow.Step.StepCase.REENCRYPT_STEP ->
        CryptorExchangeTask.forReEncryption(deterministicCommutativeCryptor)
      ExchangeWorkflow.Step.StepCase.DECRYPT_STEP ->
        CryptorExchangeTask.forDecryption(deterministicCommutativeCryptor)
      else -> error("Unsupported step type")
    }
  }

  suspend fun execute(
    apiClient: ApiClient,
    attemptKey: ExchangeStepAttempt.Key,
    step: ExchangeWorkflow.Step
  ) = coroutineScope {
    try {
      logger.addToTaskLog("Executing $step with attempt $attemptKey")
      withTimeout(timeoutDuration.toMillis()) {
        if (step.getStepCase() == ExchangeWorkflow.Step.StepCase.INPUT_STEP) {
          InputTask(
              preferredSharedStorage = preferredSharedStorage,
              preferredPrivateStorage = preferredPrivateStorage,
              step = step,
              retryDuration = retryDuration
            )
            .execute(emptyMap<String, ByteString>())
        } else {
          val taskInput: Map<String, ByteString> =
            getAllInputForStep(
              preferredSharedStorage = preferredSharedStorage,
              preferredPrivateStorage = preferredPrivateStorage,
              step = step
            )
          val taskOutput: Map<String, ByteString> = getExchangeTaskForStep(step).execute(taskInput)
          writeAllOutputForStep(
            preferredSharedStorage = preferredSharedStorage,
            preferredPrivateStorage = preferredPrivateStorage,
            step = step,
            taskOutput = taskOutput
          )
        }
      }
      apiClient.finishExchangeStepAttempt(
        attemptKey,
        ExchangeStepAttempt.State.SUCCEEDED,
        logger.getAndClearTaskLog()
      )
    } catch (e: Exception) {
      logger.addToTaskLog(e.toString())
      apiClient.finishExchangeStepAttempt(
        attemptKey,
        ExchangeStepAttempt.State.FAILED,
        logger.getAndClearTaskLog()
      )
      throw e
    }
  }

  companion object {
    val logger by loggerFor()
  }
}
