package org.wfanet.panelmatch.client.exchangetasks

import org.wfanet.panelmatch.client.common.ExchangeContext
import org.wfanet.panelmatch.common.crypto.DeterministicCommutativeCipher
import src.main.kotlin.org.wfanet.panelmatch.client.exchangetasks.CommutativeEncryptionTasks

class JniCommutativeEncryptionTasks(
  private val deterministicCommutativeCryptor: DeterministicCommutativeCipher
) : CommutativeEncryptionTasks {
  override fun ExchangeContext.buildEncryptTask(): ExchangeTask {
    return CryptorExchangeTask.forEncryption(deterministicCommutativeCryptor)
  }

  override fun ExchangeContext.buildDecryptTask(): ExchangeTask {
    return CryptorExchangeTask.forDecryption(deterministicCommutativeCryptor)
  }

  override fun ExchangeContext.buildReEncryptTask(): ExchangeTask {
    return CryptorExchangeTask.forReEncryption(deterministicCommutativeCryptor)
  }

  override fun ExchangeContext.buildGenerateCommutativeEncryptionKeyTask(): ExchangeTask {
    return GenerateSymmetricKeyTask(generateKey = deterministicCommutativeCryptor::generateKey)
  }
}
