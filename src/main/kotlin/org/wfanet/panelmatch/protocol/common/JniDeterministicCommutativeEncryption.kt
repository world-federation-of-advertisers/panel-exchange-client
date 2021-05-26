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

package org.wfanet.panelmatch.protocol.common

import java.lang.RuntimeException
import java.nio.file.Paths
import org.wfanet.panelmatch.common.loadLibrary
import wfanet.panelmatch.protocol.crypto.DeterministicCommutativeEncryptionUtility
import wfanet.panelmatch.protocol.protobuf.ApplyDecryptionRequest
import wfanet.panelmatch.protocol.protobuf.ApplyDecryptionResponse
import wfanet.panelmatch.protocol.protobuf.ApplyEncryptionRequest
import wfanet.panelmatch.protocol.protobuf.ApplyEncryptionResponse
import wfanet.panelmatch.protocol.protobuf.ReApplyEncryptionRequest
import wfanet.panelmatch.protocol.protobuf.ReApplyEncryptionResponse

/**
 * A [DeterministicCommutativeEncryption] implementation using the JNI
 * [DeterministicCommutativeEncryptionUtility].
 */
class JniDeterministicCommutativeEncryption : Encryption {
  /** Indicates something went wrong in C++. */
  class JniException(cause: Throwable) : RuntimeException(cause)

  private fun <T> wrapJniException(block: () -> T): T {
    return try {
      block()
    } catch (e: RuntimeException) {
      throw JniException(e)
    }
  }

  override fun encrypt(request: ApplyEncryptionRequest): ApplyEncryptionResponse {
    return wrapJniException {
      ApplyEncryptionResponse.parseFrom(
        DeterministicCommutativeEncryptionUtility.applyDeterministicCommutativeEncryptionWrapper(
          request.toByteArray()
        )
      )
    }
  }

  override fun reEncrypt(request: ReApplyEncryptionRequest): ReApplyEncryptionResponse {
    return wrapJniException {
      ReApplyEncryptionResponse.parseFrom(
        DeterministicCommutativeEncryptionUtility.reApplyDeterministicCommutativeEncryptionWrapper(
          request.toByteArray()
        )
      )
    }
  }

  override fun decrypt(request: ApplyDecryptionRequest): ApplyDecryptionResponse {
    return wrapJniException {
      ApplyDecryptionResponse.parseFrom(
        DeterministicCommutativeEncryptionUtility.applyDeterministicCommutativeDecryptionWrapper(
          request.toByteArray()
        )
      )
    }
  }

  companion object {
    init {
      loadLibrary(
        name = "deterministic_commutative_encryption_utility",
        directoryPath =
          Paths.get("panel_exchange_client/src/main/swig/wfanet/panelmatch/protocol/crypto")
      )
    }
  }
}
