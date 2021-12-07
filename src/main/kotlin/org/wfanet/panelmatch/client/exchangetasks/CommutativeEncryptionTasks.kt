package src.main.kotlin.org.wfanet.panelmatch.client.exchangetasks

import org.wfanet.panelmatch.client.common.ExchangeContext
import org.wfanet.panelmatch.client.exchangetasks.ExchangeTask

interface CommutativeEncryptionTasks {
  fun ExchangeContext.buildDecryptTask(): ExchangeTask
  fun ExchangeContext.buildEncryptTask(): ExchangeTask
  fun ExchangeContext.buildReEncryptTask(): ExchangeTask
  fun ExchangeContext.buildGenerateCommutativeEncryptionKeyTask(): ExchangeTask
}
