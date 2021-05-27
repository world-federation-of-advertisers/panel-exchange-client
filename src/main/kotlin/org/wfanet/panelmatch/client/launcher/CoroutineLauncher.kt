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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.wfanet.measurement.api.v2alpha.ExchangeStep
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttempt
import org.wfanet.panelmatch.client.exchangetasks.ExchangeTaskMapper
import org.wfanet.panelmatch.client.storage.InMemoryStorage
import org.wfanet.panelmatch.client.utils.loggerFor

/** Executes an [ExchangeStep] using a couroutine. */
class CoroutineLauncher : JobLauncher {
  private val LOGGER = loggerFor(javaClass)
  private val scope = CoroutineScope(Dispatchers.Default)

  private fun executeStep(
    apiClient: ApiClient,
    exchangeStep: ExchangeStep,
    attempt: ExchangeStepAttempt.Key
  ) =
    scope.launch {
      LOGGER.info("Executing ${exchangeStep.toString()} with attempt ${attempt.toString()}")
      val storage = InMemoryStorage()
      val output =
        ExchangeTaskMapper().execute(exchangeStep.step, emptyMap<String, ByteString>(), storage)
      // val request =
      /*        FinishExchangeStepAttemptRequest.newBuilder()
      .also {
        it.key = attempt
        it.finalState = ExchangeStepAttempt.State.SUCCEEDED
      }
      .build()*/
      val logs = emptyList<String>()
      apiClient.finishExchangeStepAttempt(attempt, ExchangeStepAttempt.State.SUCCEEDED, logs)
    }

  // TODO: pass in a jobConfig for the correct storage to use
  override suspend fun execute(
    apiClient: ApiClient,
    exchangeStep: ExchangeStep,
    attempt: ExchangeStepAttempt.Key
  ) {
    executeStep(apiClient, exchangeStep, attempt)
  }
}
