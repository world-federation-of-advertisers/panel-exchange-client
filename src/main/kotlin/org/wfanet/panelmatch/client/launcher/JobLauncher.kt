package org.wfanet.panelmatch.client.launcher

import org.wfanet.measurement.api.v2alpha.ExchangeStep
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttempt

/** Executes an [ExchangeStep]. This may be locally or remotely. */
interface JobLauncher {
  /**
   * Initiates work on [exchangeStep].
   *
   * This may return before the work is complete.
   *
   * This could run [exchangeStep] in-process or enqueue/start work remotely, e.g. via an RPC call.
   */
  suspend fun execute(exchangeStep: ExchangeStep, attempt: ExchangeStepAttempt.Key)
}
