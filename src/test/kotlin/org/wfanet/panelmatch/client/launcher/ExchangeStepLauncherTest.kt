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

import com.google.common.truth.extensions.proto.ProtoTruth.assertThat
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.stub
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyBlocking
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.measurement.api.v2alpha.ExchangeStep
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttempt
import org.wfanet.panelmatch.client.launcher.ApiClient.ClaimedExchangeStep

private val EXCHANGE_STEP: ExchangeStep =
  ExchangeStep.newBuilder()
    .apply {
      keyBuilder.apply {
        recurringExchangeId = "some-recurring-exchange-id"
        exchangeId = "some-exchange-id"
        exchangeStepId = "some-step-id"
      }
      state = ExchangeStep.State.READY_FOR_RETRY
    }
    .build()

private val EXCHANGE_STEP_ATTEMPT_KEY: ExchangeStepAttempt.Key =
  ExchangeStepAttempt.Key.newBuilder()
    .apply {
      recurringExchangeId = EXCHANGE_STEP.key.recurringExchangeId
      exchangeId = EXCHANGE_STEP.key.exchangeId
      stepId = EXCHANGE_STEP.key.exchangeStepId
      exchangeStepAttemptId = "some-attempt-id"
    }
    .build()

@RunWith(JUnit4::class)
class ExchangeStepLauncherTest {
  private val apiClient: ApiClient = mock()
  private val validator: ExchangeStepValidator = mock()
  private val jobLauncher: JobLauncher = mock()

  @Test
  fun `findAndRunExchangeStep with no ExchangeTask`() {
    apiClient.stub { onBlocking { claimExchangeStep() }.thenReturn(null) }

    val launcher = ExchangeStepLauncher(apiClient, validator, jobLauncher)
    runBlocking { launcher.findAndRunExchangeStep() }

    verifyBlocking(apiClient, times(1)) { claimExchangeStep() }
    verifyZeroInteractions(validator, jobLauncher)
  }

  @Test
  fun `findAndRunExchangeStep with valid ExchangeTask`() {
    apiClient.stub {
      onBlocking { claimExchangeStep() }
        .thenReturn(ClaimedExchangeStep(EXCHANGE_STEP, EXCHANGE_STEP_ATTEMPT_KEY))
    }

    val launcher = ExchangeStepLauncher(apiClient, validator, jobLauncher)
    runBlocking { launcher.findAndRunExchangeStep() }

    verifyBlocking(apiClient, times(1)) { claimExchangeStep() }

    argumentCaptor<ExchangeStep> {
      verify(validator, times(1)).validate(capture())
      assertThat(firstValue).isEqualTo(EXCHANGE_STEP)
    }

    val (exchangeStepCaptor, attemptCaptor) =
      argumentCaptor(ExchangeStep::class, ExchangeStepAttempt.Key::class)
    verifyBlocking(jobLauncher, times(1)) {
      execute(exchangeStepCaptor.capture(), attemptCaptor.capture())
    }
    assertThat(exchangeStepCaptor.firstValue).isEqualTo(EXCHANGE_STEP)
    assertThat(attemptCaptor.firstValue).isEqualTo(EXCHANGE_STEP_ATTEMPT_KEY)
  }
}
