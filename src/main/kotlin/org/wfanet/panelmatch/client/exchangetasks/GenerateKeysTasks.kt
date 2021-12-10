package org.wfanet.panelmatch.client.exchangetasks

import org.wfanet.panelmatch.client.common.ExchangeContext

/** GenerateKeys Tasks consists of tasks that generates keys or a certificate. */
interface GenerateKeysTasks {
  /** Returns the task that generates a symmetric key. */
  fun generateSymmetricKey(context: ExchangeContext): ExchangeTask

  /** Returns the task that generates serialized rlwe keys. */
  fun generateSerializedRlweKeys(context: ExchangeContext): ExchangeTask

  /** Returns the task that generates a certificate. */
  fun generateExchangeCertificate(context: ExchangeContext): ExchangeTask
}
