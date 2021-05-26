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

package org.wfanet.panelmatch.protocol.common.testing

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import java.lang.RuntimeException
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import org.junit.Test
import org.wfanet.panelmatch.protocol.common.Encryption
import wfanet.panelmatch.protocol.protobuf.ApplyDecryptionRequest
import wfanet.panelmatch.protocol.protobuf.ApplyEncryptionRequest
import wfanet.panelmatch.protocol.protobuf.ReApplyEncryptionRequest

private val PLAINTEXTS: List<ByteString> =
  listOf<ByteString>(
    ByteString.copyFromUtf8("some plaintext0"),
    ByteString.copyFromUtf8("some plaintext1"),
    ByteString.copyFromUtf8("some plaintext2"),
    ByteString.copyFromUtf8("some plaintext3"),
    ByteString.copyFromUtf8("some plaintext4")
  )

private val CIPHERTEXTS: List<ByteString> =
  listOf<ByteString>(
    ByteString.copyFromUtf8("some ciphertext0"),
    ByteString.copyFromUtf8("some ciphertext1")
  )

/** Abstract base class for testing implementations of [Encryption]. */
abstract class AbstractEncryptionTest {
  abstract val Encryption: Encryption

  @Test
  fun testEncryption() {
    val randomKey1: ByteString = ByteString.copyFromUtf8("random-key-00")
    val randomKey2: ByteString = ByteString.copyFromUtf8("random-key-222")

    val encryptedTexts1 = Encryption.encrypt(randomKey1, PLAINTEXTS)
    val encryptedTexts2 = Encryption.encrypt(randomKey2, PLAINTEXTS)
    assertThat(encryptedTexts1).isNotEqualTo(encryptedTexts2)

    val reEncryptedTexts1 = Encryption.reEncrypt(randomKey1, encryptedTexts2)
    val reEncryptedTexts2 = Encryption.reEncrypt(randomKey2, encryptedTexts1)
    assertThat(reEncryptedTexts1).isNotEqualTo(encryptedTexts2)
    assertThat(reEncryptedTexts2).isNotEqualTo(encryptedTexts1)

    val decryptedTexts1 = Encryption.decrypt(randomKey1, reEncryptedTexts1)
    val decryptedTexts2 = Encryption.decrypt(randomKey1, reEncryptedTexts2)
    assertThat(decryptedTexts1).isEqualTo(encryptedTexts2)
    assertThat(decryptedTexts2).isEqualTo(encryptedTexts2)

    val decryptedTexts3 = Encryption.decrypt(randomKey2, reEncryptedTexts1)
    val decryptedTexts4 = Encryption.decrypt(randomKey2, reEncryptedTexts2)
    assertThat(decryptedTexts3).isEqualTo(encryptedTexts1)
    assertThat(decryptedTexts4).isEqualTo(encryptedTexts1)
  }

  @Test
  fun `invalid key`() {
    val key: ByteString = ByteString.copyFromUtf8("this key is too long so it causes problems")
    assertFails { Encryption.encrypt(key, PLAINTEXTS) }
    assertFails { Encryption.decrypt(key, PLAINTEXTS) }
    assertFails { Encryption.reEncrypt(key, PLAINTEXTS) }
  }

  @Test
  fun `invalid ApplyEncryption proto`() {
    val missingKeyException =
      assertFailsWith(RuntimeException::class) {
        val request = ApplyEncryptionRequest.newBuilder().addAllPlaintexts(PLAINTEXTS).build()
        Encryption.encrypt(request)
      }
    assertThat(missingKeyException.message).contains("Failed to create the protocol cipher")
  }

  @Test
  fun `invalid ApplyDecryption proto`() {
    val missingKeyException =
      assertFailsWith(RuntimeException::class) {
        val request = ApplyDecryptionRequest.newBuilder().addAllEncryptedTexts(CIPHERTEXTS).build()
        Encryption.decrypt(request)
      }
    assertThat(missingKeyException.message).contains("Failed to create the protocol cipher")
  }

  @Test
  fun `invalid ReApplyEncryption proto`() {
    val missingKeyException =
      assertFailsWith(RuntimeException::class) {
        val request =
          ReApplyEncryptionRequest.newBuilder().addAllEncryptedTexts(CIPHERTEXTS).build()
        Encryption.reEncrypt(request)
      }
    assertThat(missingKeyException.message).contains("Failed to create the protocol cipher")
  }
}
