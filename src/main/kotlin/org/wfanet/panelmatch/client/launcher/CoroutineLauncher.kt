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

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wfanet.measurement.api.v2alpha.ExchangeStep
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttempt
import org.wfanet.panelmatch.client.exchangetasks.ExchangeTaskMapper
import org.wfanet.panelmatch.client.logger.loggerFor
import org.wfanet.panelmatch.protocol.common.Cryptor
import org.wfanet.panelmatch.protocol.common.JniDeterministicCommutativeCryptor

/** Executes an [ExchangeStep] using a couroutine. */
class CoroutineLauncher(
  private val deterministicCommutativeCryptor: Cryptor = JniDeterministicCommutativeCryptor()
) : JobLauncher {
  private val LOGGER = loggerFor(javaClass)
  private val scope = CoroutineScope(Dispatchers.Default)

  private fun executeStep(
    apiClient: ApiClient,
    exchangeKey: String,
    exchangeStepAttemptKey: String,
    exchangeStep: ExchangeStep,
    attempt: ExchangeStepAttempt.Key
  ): Job {
    // All Jobs should be launched to a coroutine with a name associated with attemptKey for easy
    // later debugging
    return scope.launch(CoroutineName(exchangeStepAttemptKey) + Dispatchers.Default) {
      try {
        LOGGER.addToTaskLog(
          "Executing ${exchangeStepAttemptKey}:${exchangeStep.toString()} with attempt ${attempt.toString()}"
        )
        ExchangeTaskMapper(deterministicCommutativeCryptor).execute(exchangeKey, exchangeStep.step)
        apiClient.finishExchangeStepAttempt(
          attempt,
          ExchangeStepAttempt.State.SUCCEEDED,
          LOGGER.getAndClearTaskLog()
        )
      } catch (e: Exception) {
        LOGGER.addToTaskLog(e.toString())
        apiClient.finishExchangeStepAttempt(
          attempt,
          ExchangeStepAttempt.State.FAILED,
          LOGGER.getAndClearTaskLog()
        )
      }
    }
  }

  override suspend fun execute(
    apiClient: ApiClient,
    exchangeKey: String,
    exchangeStepAttemptKey: String,
    exchangeStep: ExchangeStep,
    attempt: ExchangeStepAttempt.Key
  ) {
    val job: Job =
      executeStep(apiClient, exchangeKey, exchangeStepAttemptKey, exchangeStep, attempt)
  }
}
