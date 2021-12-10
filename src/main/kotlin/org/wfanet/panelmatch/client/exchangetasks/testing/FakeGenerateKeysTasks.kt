package org.wfanet.panelmatch.client.exchangetasks.testing

import org.wfanet.panelmatch.client.common.ExchangeContext
import org.wfanet.panelmatch.client.exchangetasks.GenerateKeysTasks

class FakeGenerateKeysTasks : GenerateKeysTasks {
  override fun generateSymmetricKey(context: ExchangeContext) = FakeExchangeTask("symmetric-key")
  override fun generateSerializedRlweKeys(context: ExchangeContext) =
    FakeExchangeTask("serialized-rlwe-keys")
  override fun generateExchangeCertificate(context: ExchangeContext) =
    FakeExchangeTask("exchange-certificate")
}
