package org.wfanet.panelmatch.client.exchangetasks.testing

import org.wfanet.panelmatch.client.common.ExchangeContext
import org.wfanet.panelmatch.client.exchangetasks.PrivateStorageTasks

class FakePrivateStorageTasks : PrivateStorageTasks {
  override suspend fun getInput(context: ExchangeContext) = FakeExchangeTask("get-input")
  override suspend fun copyFromPreviousExchange(context: ExchangeContext) =
    FakeExchangeTask("copy-from-prev-exchange")
}
