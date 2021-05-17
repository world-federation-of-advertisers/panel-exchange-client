package org.wfanet.panelmatch.client.launcher

import org.wfanet.measurement.api.v2alpha.ExchangeStep
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttempt

/** Abstracts interactions with the centralized Panel Match APIs. */
interface ApiClient {
  data class ClaimedExchangeStep(
    val exchangeStep: ExchangeStep,
    val exchangeStepAttempt: ExchangeStepAttempt.Key
  )

  /**
   * Attempts to fetch an [ExchangeStep] to work on.
   *
   * @return an unvalidated [ExchangeStep] ready to work on -- or null if none exist.
   */
  suspend fun claimExchangeStep(): ClaimedExchangeStep?

  /** Attaches debug log entries to an [ExchangeStepAttempt]. */
  suspend fun appendLogEntry(key: ExchangeStepAttempt.Key, vararg messages: String)

  /** Marks an ExchangeStepAttempt as complete (successfully or otherwise). */
  suspend fun finishExchangeStepAttempt(
    key: ExchangeStepAttempt.Key,
    finalState: ExchangeStepAttempt.State,
    vararg logEntryMessages: String
  )
}
