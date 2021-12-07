package org.wfanet.panelmatch.client.exchangetasks

import org.wfanet.panelmatch.client.common.ExchangeContext

interface PrivateStorageTasks {
  suspend fun ExchangeContext.getInputStepTask(): ExchangeTask
  suspend fun ExchangeContext.getCopyFromPreviousExchangeTask(): ExchangeTask
}
