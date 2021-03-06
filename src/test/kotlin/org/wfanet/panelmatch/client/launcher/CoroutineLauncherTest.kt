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

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttemptKey
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflowKt.StepKt.commutativeDeterministicEncryptStep
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflowKt.step
import org.wfanet.measurement.common.CountDownLatch
import org.wfanet.panelmatch.client.launcher.ExchangeStepValidator.ValidatedExchangeStep
import org.wfanet.panelmatch.client.launcher.testing.buildWorkflow
import org.wfanet.panelmatch.common.testing.runBlockingTest

@RunWith(JUnit4::class)
class CoroutineLauncherTest {
  private val stepExecutor = mock<ExchangeStepExecutor>()
  private val launcher = CoroutineLauncher(stepExecutor = stepExecutor)

  @Test
  fun launches() = runBlockingTest {
    val workflowStep = step {
      this.commutativeDeterministicEncryptStep = commutativeDeterministicEncryptStep {}
    }
    val workflow = buildWorkflow(workflowStep, "some-edp", "some-mp")

    val startLatch = CountDownLatch(1)
    val middleLatch = CountDownLatch(1)
    val endLatch = CountDownLatch(1)
    whenever(stepExecutor.execute(any(), any())).thenAnswer {
      runBlocking {
        startLatch.countDown()
        middleLatch.await()
        endLatch.countDown()
      }
    }

    val attemptKey = ExchangeStepAttemptKey("w", "x", "y", "z")
    val date = LocalDate.of(2021, 11, 29)

    val validatedExchangeStep = ValidatedExchangeStep(workflow, workflow.getSteps(0), date)

    launcher.execute(validatedExchangeStep, attemptKey)

    startLatch.await()
    middleLatch.countDown()
    endLatch.await()

    val stepCaptor = argumentCaptor<ValidatedExchangeStep>()
    val attemptKeyCaptor = argumentCaptor<ExchangeStepAttemptKey>()
    verify(stepExecutor).execute(stepCaptor.capture(), attemptKeyCaptor.capture())
    assertThat(stepCaptor.firstValue).isEqualTo(validatedExchangeStep)
    assertThat(attemptKeyCaptor.firstValue).isEqualTo(attemptKey)
  }
}
