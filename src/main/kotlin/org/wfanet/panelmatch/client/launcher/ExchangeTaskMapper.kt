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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttempt
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.panelmatch.client.exchangetasks.ExchangeTask
import org.wfanet.panelmatch.client.logger.addToTaskLog
import org.wfanet.panelmatch.client.logger.getAndClearTaskLog
import org.wfanet.panelmatch.client.logger.loggerFor
import org.wfanet.panelmatch.client.storage.Storage

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
  private val apiClient: ApiClient,
  private val timeoutDuration: Duration = Duration.ofDays(1),
  private val preferredSharedStorage: Storage,
  private val preferredPrivateStorage: Storage,
  private val getExchangeTaskForStep: (ExchangeWorkflow.Step) -> ExchangeTask
) {

  /**
   * Reads private and shared task input from different storage based on [exchangeKey] and
   * [ExchangeStep] and returns Map<String, ByteString> of different input
   */
  suspend fun getAllInputForStep(
    preferredSharedStorage: Storage,
    preferredPrivateStorage: Storage,
    step: ExchangeWorkflow.Step
  ): Map<String, ByteString> = coroutineScope {
    val privateInputLabels = step.getPrivateInputLabelsMap()
    val sharedInputLabels = step.getSharedInputLabelsMap()
    awaitAll(
      async(start = CoroutineStart.DEFAULT) {
        preferredPrivateStorage.batchRead(inputLabels = privateInputLabels)
      },
      async(start = CoroutineStart.DEFAULT) {
        preferredSharedStorage.batchRead(inputLabels = sharedInputLabels)
      }
    )
      .reduce { a, b -> a.toMutableMap().apply { putAll(b) } }
  }

  /**
   * Writes private and shared task output [taskOutput] to different storage based on [exchangeKey]
   * and [ExchangeStep].
   */
  private suspend fun writeAllOutputForStep(
    preferredSharedStorage: Storage,
    preferredPrivateStorage: Storage,
    step: ExchangeWorkflow.Step,
    taskOutput: Map<String, ByteString>
  ) = coroutineScope {
    val privateOutputLabels = step.getPrivateOutputLabelsMap()
    val sharedOutputLabels = step.getSharedOutputLabelsMap()
    async {
      preferredPrivateStorage.batchWrite(outputLabels = privateOutputLabels, data = taskOutput)
    }
    async {
      preferredSharedStorage.batchWrite(outputLabels = sharedOutputLabels, data = taskOutput)
    }
  }

  suspend fun execute(attemptKey: ExchangeStepAttempt.Key, step: ExchangeWorkflow.Step) =
      coroutineScope {
    try {
      logger.addToTaskLog("Executing $step with attempt $attemptKey")
      val exchangeTask: ExchangeTask = getExchangeTaskForStep(step)
      withTimeout(timeoutDuration.toMillis()) {
        if (step.getStepCase() == ExchangeWorkflow.Step.StepCase.INPUT_STEP) {
          exchangeTask.execute(emptyMap<String, ByteString>())
        } else {
          val taskInput: Map<String, ByteString> =
            getAllInputForStep(
              preferredSharedStorage = preferredSharedStorage,
              preferredPrivateStorage = preferredPrivateStorage,
              step = step
            )
          val taskOutput: Map<String, ByteString> = exchangeTask.execute(taskInput)
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
