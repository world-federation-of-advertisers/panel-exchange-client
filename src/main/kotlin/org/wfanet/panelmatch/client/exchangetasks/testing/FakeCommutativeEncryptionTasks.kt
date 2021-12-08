package org.wfanet.panelmatch.client.exchangetasks.testing

import org.wfanet.panelmatch.client.exchangetasks.CommutativeEncryptionTasks

class FakeCommutativeEncryptionTasks : CommutativeEncryptionTasks {
  override fun encrypt() = FakeExchangeTask("encrypt")
  override fun reEncrypt() = FakeExchangeTask("re-encrypt")
  override fun decrypt() = FakeExchangeTask("decrypt")
  override fun generateCommutativeEncryptionKey() = FakeExchangeTask("generate-encryption-key")
}
