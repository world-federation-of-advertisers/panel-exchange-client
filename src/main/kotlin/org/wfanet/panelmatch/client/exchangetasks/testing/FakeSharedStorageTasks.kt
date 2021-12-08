package org.wfanet.panelmatch.client.exchangetasks.testing

import org.wfanet.panelmatch.client.common.ExchangeContext
import org.wfanet.panelmatch.client.exchangetasks.SharedStorageTasks

class FakeSharedStorageTasks : SharedStorageTasks {
  override suspend fun copyFromSharedStorage(context: ExchangeContext) =
    FakeExchangeTask("copy-from-shared-storage")
  override suspend fun copyToSharedStorage(context: ExchangeContext) =
    FakeExchangeTask("copy-to-shared-storage")
}
