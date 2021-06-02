package org.wfanet.panelmatch.client.launcher

import org.wfanet.measurement.api.v2alpha.ExchangeStep
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttempt

class BlockingJobLauncher : JobLauncher {
  // TODO: implement execute method.
  override suspend fun execute(exchangeStep: ExchangeStep, attempt: ExchangeStepAttempt.Key) {}
}
