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

package org.wfanet.panelmatch.client.joinkeyexchange.testing

import com.google.protobuf.ByteString
import org.wfanet.panelmatch.client.joinkeyexchange.JoinKeyAndId
import org.wfanet.panelmatch.client.joinkeyexchange.JoinKeyCryptor
import org.wfanet.panelmatch.client.joinkeyexchange.joinKey
import org.wfanet.panelmatch.client.joinkeyexchange.joinKeyAndId
import org.wfanet.panelmatch.common.toByteString

private const val SEPARATOR = " encrypted by "

/** For testing only. Does not play nicely with non-Utf8 source data. */
object FakeJoinKeyCryptor : JoinKeyCryptor {
  val INVALID_KEY = "invalid key".toByteString()

  override fun generateKey(): ByteString {
    var key = ""
    for (i in 1..20) {
      key += ('A'..'Z').random()
    }
    return key.toByteString()
  }

  override fun encrypt(privateKey: ByteString, plaintexts: List<JoinKeyAndId>): List<JoinKeyAndId> {
    require(privateKey != INVALID_KEY) { "Invalid Key" }
    return plaintexts.map { plaintext ->
      joinKeyAndId {
        this.joinKey =
          joinKey {
            key = plaintext.joinKey.key.concat(SEPARATOR.toByteString()).concat(privateKey)
          }
        joinKeyIdentifier = plaintext.joinKeyIdentifier
      }
    }
  }

  override fun reEncrypt(
    privateKey: ByteString,
    ciphertexts: List<JoinKeyAndId>
  ): List<JoinKeyAndId> {
    require(privateKey != INVALID_KEY) { "Invalid Key" }
    return ciphertexts.map { ciphertext ->
      require(ciphertext.joinKey.key.toStringUtf8().contains(SEPARATOR)) { "invalid ciphertext" }
      joinKeyAndId {
        this.joinKey =
          joinKey {
            key = ciphertext.joinKey.key.concat(SEPARATOR.toByteString()).concat(privateKey)
          }
        joinKeyIdentifier = ciphertext.joinKeyIdentifier
      }
    }
  }

  override fun decrypt(
    privateKey: ByteString,
    ciphertexts: List<JoinKeyAndId>
  ): List<JoinKeyAndId> {
    require(privateKey != INVALID_KEY) { "Invalid Key" }
    val encryptionString = SEPARATOR + privateKey.toStringUtf8()
    return ciphertexts.map { ciphertext ->
      val dataString = ciphertext.joinKey.key.toStringUtf8()
      require(dataString.contains(encryptionString)) { "invalid ciphertext" }
      joinKeyAndId {
        this.joinKey = joinKey { key = dataString.replace(encryptionString, "").toByteString() }
        joinKeyIdentifier = ciphertext.joinKeyIdentifier
      }
    }
  }
}
