// Copyright 2021 The Cross-Media Measurement Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.wfanet.panelmatch.client.exchangetasks

import org.wfanet.panelmatch.client.common.ExchangeContext
import org.wfanet.panelmatch.common.crypto.DeterministicCommutativeCipher

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