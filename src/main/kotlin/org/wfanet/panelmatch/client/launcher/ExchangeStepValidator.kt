package org.wfanet.panelmatch.client.launcher

import org.wfanet.measurement.api.v2alpha.ExchangeStep

/** Indicates that an [ExchangeStep] is not valid to execute. */
class InvalidExchangeStepException(message: String) : Exception(message)

/** Determines whether an ExchangeStep is valid and can be safely executed. */
interface ExchangeStepValidator {
  /** Throws [InvalidExchangeStepException] if [exchangeStep] is invalid. */
  fun validate(exchangeStep: ExchangeStep)
}
