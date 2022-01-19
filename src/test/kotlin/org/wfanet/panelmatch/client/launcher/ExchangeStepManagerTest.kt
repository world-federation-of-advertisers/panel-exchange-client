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
import com.google.common.truth.extensions.proto.ProtoTruth.assertThat
import java.time.LocalDate
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.verifyZeroInteractions
import org.mockito.kotlin.whenever
import org.wfanet.measurement.api.v2alpha.ExchangeStep
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttemptKey
import org.wfanet.measurement.api.v2alpha.ExchangeStepKey
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflowKt.step
import org.wfanet.measurement.api.v2alpha.exchangeStep
import org.wfanet.measurement.api.v2alpha.exchangeWorkflow
import org.wfanet.measurement.common.toProtoDate
import org.wfanet.panelmatch.client.launcher.ExchangeStepValidator.ValidatedExchangeStep
import org.wfanet.panelmatch.client.launcher.InvalidExchangeStepException.FailureType.PERMANENT
import org.wfanet.panelmatch.client.launcher.InvalidExchangeStepException.FailureType.TRANSIENT
import org.wfanet.panelmatch.common.testing.runBlockingTest
import org.wfanet.panelmatch.protocol.claimedExchangeStep

private const val RECURRING_EXCHANGE_ID = "some-recurring-exchange-id"
private const val EXCHANGE_ID = "some-exchange-id"
private const val EXCHANGE_STEP_ID = "some-step-id"
private const val EXCHANGE_STEP_ATTEMPT_ID = "some-attempt-id"

private val EXCHANGE_STEP_ATTEMPT_KEY: ExchangeStepAttemptKey =
  ExchangeStepAttemptKey(
    recurringExchangeId = RECURRING_EXCHANGE_ID,
    exchangeId = EXCHANGE_ID,
    exchangeStepId = EXCHANGE_STEP_ID,
    exchangeStepAttemptId = EXCHANGE_STEP_ATTEMPT_ID
  )

private val EXCHANGE_STEP_KEY =
  ExchangeStepKey(
    recurringExchangeId = RECURRING_EXCHANGE_ID,
    exchangeId = EXCHANGE_ID,
    exchangeStepId = EXCHANGE_STEP_ID
  )

private val DATE = LocalDate.now()

private val WORKFLOW = exchangeWorkflow {
  steps += step {}
  steps += step {}
  steps += step { stepId = "this-step-will-run" }
  steps += step {}
}

private val EXCHANGE_STEP: ExchangeStep = exchangeStep {
  name = EXCHANGE_STEP_KEY.toName()
  state = ExchangeStep.State.READY_FOR_RETRY
  stepIndex = 2
  serializedExchangeWorkflow = WORKFLOW.toByteString()
  exchangeDate = DATE.toProtoDate()
}

private val VALIDATED_EXCHANGE_STEP = ValidatedExchangeStep(WORKFLOW, WORKFLOW.getSteps(2), DATE)

private val CLAIMED_EXCHANGE_STEP = claimedExchangeStep {
  step = EXCHANGE_STEP
  stepAttemptKey = EXCHANGE_STEP_ATTEMPT_KEY.toName()
}
private val generateJobId = { "some-job-id" }
private val JOB_ID by lazy { generateJobId() }

@RunWith(JUnit4::class)
class ExchangeStepManagerTest {
  private val validator: ExchangeStepValidator = mock()
  private val jobLauncher: JobLauncher = mock()
  private val reporter: ExchangeStepReporter = mock()
  private val manager = ExchangeStepManager(validator, jobLauncher, reporter, generateJobId)

  @Test
  fun noExchangeTask() = runBlockingTest {
    whenever(reporter.getAndStoreClaimStatus(JOB_ID)).thenReturn(null)

    manager.manageStep()

    verifyBlocking(reporter) { getAndStoreClaimStatus(JOB_ID) }
    verifyZeroInteractions(jobLauncher, validator)
  }

  @Test
  fun validExchangeTask() = runBlockingTest {
    whenever(reporter.getAndStoreClaimStatus(JOB_ID)).thenReturn(CLAIMED_EXCHANGE_STEP)
    whenever(validator.validate(any())).thenReturn(VALIDATED_EXCHANGE_STEP)
    whenever(reporter.getClaimStatus(JOB_ID)).thenReturn(CLAIMED_EXCHANGE_STEP)

    manager.manageStep()

    verifyBlocking(jobLauncher) {
      val jobIdCaptor = argumentCaptor<String>()
      val stepCaptor = argumentCaptor<ValidatedExchangeStep>()
      val attemptCaptor = argumentCaptor<ExchangeStepAttemptKey>()

      execute(jobIdCaptor.capture(), stepCaptor.capture(), attemptCaptor.capture())

      assertThat(jobIdCaptor.firstValue).isEqualTo(JOB_ID)
      assertThat(stepCaptor.firstValue).isEqualTo(VALIDATED_EXCHANGE_STEP)
      assertThat(attemptCaptor.firstValue).isEqualTo(EXCHANGE_STEP_ATTEMPT_KEY)
    }

    verifyBlocking(validator) {
      val exchangeStepCaptor = argumentCaptor<ExchangeStep>()

      validate(exchangeStepCaptor.capture())

      assertThat(exchangeStepCaptor.firstValue).isEqualTo(EXCHANGE_STEP)
    }
  }

  @Test
  fun permanentInvalidExchangeStepException() = runBlockingTest {
    whenever(reporter.getAndStoreClaimStatus(JOB_ID)).thenReturn(CLAIMED_EXCHANGE_STEP)

    val message = "some-message"
    whenever(validator.validate(any())).thenThrow(InvalidExchangeStepException(PERMANENT, message))

    manager.manageStep()

    verifyBlocking(reporter) {
      val jobIdCaptor = argumentCaptor<String>()

      reportExecutionStatus(jobIdCaptor.capture())

      assertThat(jobIdCaptor.firstValue).isEqualTo(JOB_ID)
    }

    verifyZeroInteractions(jobLauncher)
  }

  @Test
  fun transientInvalidExchangeStepException() = runBlockingTest {
    whenever(reporter.getAndStoreClaimStatus(JOB_ID)).thenReturn(CLAIMED_EXCHANGE_STEP)

    val message = "some-message"
    whenever(validator.validate(any())).thenThrow(InvalidExchangeStepException(TRANSIENT, message))

    manager.manageStep()

    verifyBlocking(reporter) {
      val jobIdCaptor = argumentCaptor<String>()

      reportExecutionStatus(jobIdCaptor.capture())

      assertThat(jobIdCaptor.firstValue).isEqualTo(JOB_ID)
    }

    verifyZeroInteractions(jobLauncher)
  }

  @Test
  fun genericExceptionInLauncher() = runBlockingTest {
    whenever(reporter.getAndStoreClaimStatus(JOB_ID)).thenReturn(CLAIMED_EXCHANGE_STEP)
    whenever(validator.validate(any())).thenReturn(VALIDATED_EXCHANGE_STEP)

    val message = "some-message"
    whenever(jobLauncher.execute(any(), any(), any())).thenThrow(RuntimeException(message))

    manager.manageStep()

    verifyBlocking(reporter) {
      val jobIdCaptor = argumentCaptor<String>()

      reportExecutionStatus(jobIdCaptor.capture())

      assertThat(jobIdCaptor.firstValue).isEqualTo(JOB_ID)
    }
  }
}
