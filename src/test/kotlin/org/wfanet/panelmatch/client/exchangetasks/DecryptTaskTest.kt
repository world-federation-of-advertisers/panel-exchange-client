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

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.panelmatch.protocol.common.CommutativeEncryption
import org.wfanet.panelmatch.protocol.common.makeSerializedSharedInputs

private val KEY: ByteString = ByteString.copyFromUtf8("some-key")

private val PLAINTEXTS: List<ByteString> =
  listOf<ByteString>(
    ByteString.copyFromUtf8("plaintext1"),
    ByteString.copyFromUtf8("plaintext2"),
    ByteString.copyFromUtf8("plaintext3")
  )

private val CIPHERTEXTS: List<ByteString> =
  listOf<ByteString>(
    ByteString.copyFromUtf8("ciphertext1"),
    ByteString.copyFromUtf8("ciphertext2"),
    ByteString.copyFromUtf8("ciphertext3")
  )

@RunWith(JUnit4::class)
class DecryptTaskTest {
  private val commutativeEncryption = mock<CommutativeEncryption>()
  private val fakeSendDebugLog: suspend (String) -> Unit = {}

  @Test
  fun `decryptTask with valid inputs`() {
    whenever(commutativeEncryption.decrypt(any(), any())).thenReturn(PLAINTEXTS)

    val result = runBlocking {
      DecryptTask(commutativeEncryption)
        .execute(
          mapOf(
            "encryption-key" to KEY,
            "encrypted-data" to makeSerializedSharedInputs(CIPHERTEXTS)
          ),
          fakeSendDebugLog
        )
    }

    assertThat(result).containsExactly("decrypted-data", makeSerializedSharedInputs(PLAINTEXTS))
  }

  @Test
  fun `decryptTask with crypto error`() {
    whenever(commutativeEncryption.decrypt(any(), any()))
      .thenThrow(IllegalArgumentException("Something went wrong"))

    val exception = runBlocking {
      assertFailsWith(IllegalArgumentException::class) {
        DecryptTask(commutativeEncryption)
          .execute(
            mapOf(
              "encryption-key" to KEY,
              "encrypted-data" to makeSerializedSharedInputs(CIPHERTEXTS)
            ),
            fakeSendDebugLog
          )
      }
    }

    assertThat(exception.message).contains("Something went wrong")
  }

  @Test
  fun `decryptTask with missing inputs`() {
    runBlocking {
      assertFailsWith(IllegalArgumentException::class) {
        DecryptTask(commutativeEncryption)
          .execute(
            mapOf("encrypted-data" to makeSerializedSharedInputs(CIPHERTEXTS)),
            fakeSendDebugLog
          )
      }
      assertFailsWith(IllegalArgumentException::class) {
        DecryptTask(commutativeEncryption).execute(mapOf("encryption-key" to KEY), fakeSendDebugLog)
      }
    }

    verifyZeroInteractions(commutativeEncryption)
  }
}
