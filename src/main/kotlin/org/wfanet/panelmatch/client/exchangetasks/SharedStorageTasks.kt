package src.main.kotlin.org.wfanet.panelmatch.client.exchangetasks

import org.wfanet.panelmatch.client.common.ExchangeContext
import org.wfanet.panelmatch.client.exchangetasks.ExchangeTask

interface SharedStorageTasks {
  suspend fun ExchangeContext.buildCopyToSharedStorageTask(): ExchangeTask
  suspend fun ExchangeContext.buildCopyFromSharedStorageTask(): ExchangeTask
}
