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

package org.wfanet.panelmatch.client.joinkeyexchange

import com.google.protobuf.ByteString
import org.wfanet.panelmatch.common.loadLibraryFromResource
import org.wfanet.panelmatch.common.wrapJniException

private const val SWIG_PREFIX: String = "/main/swig/wfanet/panelmatch/client/joinkeyexchange"

/** A [JoinKeyCryptor] implementation using the JNI [JoinKeyCryptorWrapper]. */
class JniJoinKeyCryptor : JoinKeyCryptor {

  init {
    loadLibraryFromResource("join_key_cryptor", SWIG_PREFIX)
  }

  override fun generateKey(): ByteString {
    val request = joinKeyCryptorGenerateCipherKeyRequest {}
    val response = wrapJniException {
      JoinKeyCryptorGenerateCipherKeyResponse.parseFrom(
        JoinKeyCryptorWrapper.joinKeyCryptorGenerateCipherKeyWrapper(request.toByteArray())
      )
    }
    return response.key
  }

  override fun encrypt(privateKey: ByteString, plaintexts: List<JoinKeyAndId>): List<JoinKeyAndId> {
    val request = joinKeyCryptorEncryptRequest {
      encryptionKey = privateKey
      plaintextJoinKeyAndIds += plaintexts
    }
    val response = wrapJniException {
      JoinKeyCryptorEncryptResponse.parseFrom(
        JoinKeyCryptorWrapper.joinKeyCryptorEncryptWrapper(request.toByteArray())
      )
    }
    return response.encryptedJoinKeyAndIdsList
  }

  override fun reEncrypt(
    privateKey: ByteString,
    ciphertexts: List<JoinKeyAndId>
  ): List<JoinKeyAndId> {
    val request = joinKeyCryptorReEncryptRequest {
      encryptionKey = privateKey
      encryptedJoinKeyAndIds += ciphertexts
    }
    val response = wrapJniException {
      JoinKeyCryptorReEncryptResponse.parseFrom(
        JoinKeyCryptorWrapper.joinKeyCryptorReEncryptWrapper(request.toByteArray())
      )
    }
    return response.encryptedJoinKeyAndIdsList
  }

  override fun decrypt(
    privateKey: ByteString,
    ciphertexts: List<JoinKeyAndId>
  ): List<JoinKeyAndId> {
    val request = joinKeyCryptorDecryptRequest {
      encryptionKey = privateKey
      encryptedJoinKeyAndIds += ciphertexts
    }
    val response = wrapJniException {
      JoinKeyCryptorDecryptResponse.parseFrom(
        JoinKeyCryptorWrapper.joinKeyCryptorDecryptWrapper(request.toByteArray())
      )
    }
    return response.decryptedJoinKeyAndIdsList
  }
}
