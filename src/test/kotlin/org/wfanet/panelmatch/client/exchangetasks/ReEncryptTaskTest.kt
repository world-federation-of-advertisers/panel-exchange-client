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

private val ENCRYPTED_DATA: List<ByteString> =
  listOf<ByteString>(
    ByteString.copyFromUtf8("plaintext1"),
    ByteString.copyFromUtf8("plaintext2"),
    ByteString.copyFromUtf8("plaintext3")
  )

private val DOUBLE_ENCRYPTED_DATA: List<ByteString> =
  listOf<ByteString>(
    ByteString.copyFromUtf8("ciphertext1"),
    ByteString.copyFromUtf8("ciphertext2"),
    ByteString.copyFromUtf8("ciphertext3")
  )

@RunWith(JUnit4::class)
class ReEncryptTaskTest {
  private val commutativeEncryption = mock<CommutativeEncryption>()
  private val fakeSendDebugLog: suspend (String) -> Unit = {}

  @Test
  fun `reEncryptTask with valid inputs`() {
    whenever(commutativeEncryption.reencrypt(any(), any())).thenReturn(DOUBLE_ENCRYPTED_DATA)

    val result = runBlocking {
      ReEncryptTask(commutativeEncryption)
        .execute(
          mapOf(
            "encryption-key" to KEY,
            "encrypted-data" to makeSerializedSharedInputs(ENCRYPTED_DATA)
          ),
          fakeSendDebugLog
        )
    }

    assertThat(result)
      .containsExactly("reencrypted-data", makeSerializedSharedInputs(DOUBLE_ENCRYPTED_DATA))
  }

  @Test
  fun `reEncryptTask with crypto error`() {
    whenever(commutativeEncryption.reencrypt(any(), any()))
      .thenThrow(IllegalArgumentException("Something went wrong"))

    val exception = runBlocking {
      assertFailsWith(IllegalArgumentException::class) {
        ReEncryptTask(commutativeEncryption)
          .execute(
            mapOf(
              "encryption-key" to KEY,
              "encrypted-data" to makeSerializedSharedInputs(ENCRYPTED_DATA)
            ),
            fakeSendDebugLog
          )
      }
    }

    assertThat(exception.message).contains("Something went wrong")
  }

  @Test
  fun `reEncryptTask with missing inputs`() {
    runBlocking {
      assertFailsWith(IllegalArgumentException::class) {
        ReEncryptTask(commutativeEncryption)
          .execute(
            mapOf("encrypted-data" to makeSerializedSharedInputs(ENCRYPTED_DATA)),
            fakeSendDebugLog
          )
      }
      assertFailsWith(IllegalArgumentException::class) {
        ReEncryptTask(commutativeEncryption)
          .execute(mapOf("encryption-key" to KEY), fakeSendDebugLog)
      }
    }

    verifyZeroInteractions(commutativeEncryption)
  }
}
