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

import org.wfanet.measurement.api.v2alpha.ExchangeStep
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttempt
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttemptKey
import org.wfanet.panelmatch.client.launcher.InvalidExchangeStepException.FailureType.PERMANENT
import org.wfanet.panelmatch.client.launcher.InvalidExchangeStepException.FailureType.TRANSIENT

/** Finds an [ExchangeStep], validates it, starts executing the work, and then reports status. */
class ExchangeStepManager(
  private val validator: ExchangeStepValidator,
  private val jobLauncher: JobLauncher,
  private val exchangeStepReporter: ExchangeStepReporter,
  private val generateJobId: () -> String,
) {

  /**
   * Finds a single ready Exchange Step and starts executing. If an Exchange Step is found,
   * validates it, and starts executing. If not found simply returns. Once the step is complete, it
   * reports status.
   */
  suspend fun manageStep() {
    val jobId = generateJobId()
    val claimedStep = exchangeStepReporter.getAndStoreClaimStatus(jobId) ?: return
    try {
      val validatedExchangeStep = validator.validate(claimedStep.step)
      val stepToExecute = exchangeStepReporter.getClaimStatus(jobId)
      val exchangeStepAttemptKey = ExchangeStepAttemptKey.fromName(stepToExecute.stepAttemptKey)!!
      jobLauncher.execute(jobId, validatedExchangeStep, exchangeStepAttemptKey)
      exchangeStepReporter.reportExecutionStatus(jobId)
    } catch (e: Exception) {
      val exchangeStepAttemptKey = ExchangeStepAttemptKey.fromName(claimedStep.stepAttemptKey)!!
      invalidateAttempt(jobId, exchangeStepAttemptKey, e)
    }
  }

  private suspend fun invalidateAttempt(
    jobId: String,
    exchangeStepAttemptKey: ExchangeStepAttemptKey,
    exception: Exception
  ) {
    val state =
      when (exception) {
        is InvalidExchangeStepException ->
          when (exception.type) {
            PERMANENT -> ExchangeStepAttempt.State.FAILED_STEP
            TRANSIENT -> ExchangeStepAttempt.State.FAILED
          }
        else -> ExchangeStepAttempt.State.FAILED
      }

    // TODO: log an error or retry a few times if this fails.
    // TODO: add API-level support for some type of justification about what went wrong.
    exchangeStepReporter.storeExecutionStatus(
      jobId,
      exchangeStepAttemptKey,
      state,
      listOfNotNull(exception.message)
    )
    exchangeStepReporter.reportExecutionStatus(jobId)
  }
}
