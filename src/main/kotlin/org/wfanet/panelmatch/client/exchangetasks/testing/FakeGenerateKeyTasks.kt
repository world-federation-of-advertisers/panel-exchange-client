package org.wfanet.panelmatch.client.exchangetasks.testing

import org.wfanet.panelmatch.client.common.ExchangeContext
import org.wfanet.panelmatch.client.exchangetasks.GenerateKeysTasks

class FakeGenerateKeyTasks : GenerateKeysTasks {
  override fun generateLookupKeys() = FakeExchangeTask("lookup-key")
  override fun generateSymmetricKey() = FakeExchangeTask("symmetric-key")
  override fun generateSerializedRlweKeys(context: ExchangeContext) =
    FakeExchangeTask("serialized-rlwe-keys")
  override fun generateExchangeCertificate(context: ExchangeContext) =
    FakeExchangeTask("exchange-certificate")
}
