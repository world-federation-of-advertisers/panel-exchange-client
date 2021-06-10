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
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.panelmatch.protocol.common.Cryptor
import org.wfanet.panelmatch.protocol.common.makeSerializedSharedInputs

private val KEY = ByteString.copyFromUtf8("some-key")

private val PLAINTEXTS =
  listOf<ByteString>(
    ByteString.copyFromUtf8("plaintext1"),
    ByteString.copyFromUtf8("plaintext2"),
    ByteString.copyFromUtf8("plaintext3")
  )

private val CIPHERTEXTS =
  listOf<ByteString>(
    ByteString.copyFromUtf8("ciphertext1"),
    ByteString.copyFromUtf8("ciphertext2"),
    ByteString.copyFromUtf8("ciphertext3")
  )

private val DOUBLE_CIPHERTEXTS: List<ByteString> =
  listOf<ByteString>(
    ByteString.copyFromUtf8("twice-encrypted-ciphertext1"),
    ByteString.copyFromUtf8("twice-encrypted-ciphertext2"),
    ByteString.copyFromUtf8("twice-encrypted-ciphertext3")
  )

@RunWith(JUnit4::class)
class DeterministicCommutativeCryptorExchangeTaskTest {
  val deterministicCommutativeCryptor = mock<Cryptor>()
  val ATTEMPT_KEY = java.util.UUID.randomUUID().toString()

  @Test
  fun `decrypt with valid inputs`() {
    runBlocking {
      whenever(deterministicCommutativeCryptor.decrypt(any(), any())).thenReturn(PLAINTEXTS)

      async(CoroutineName(ATTEMPT_KEY) + Dispatchers.Default) {
          val result =
            CryptorExchangeTask.forDecryption(deterministicCommutativeCryptor)
              .execute(
                mapOf(
                  "encryption-key" to KEY,
                  "encrypted-data" to makeSerializedSharedInputs(CIPHERTEXTS)
                )
              )
          assertThat(result)
            .containsExactly("decrypted-data", makeSerializedSharedInputs(PLAINTEXTS))
        }
        .await()
    }
  }

  @Test
  fun `decrypt with crypto error`() {
    runBlocking {
      whenever(deterministicCommutativeCryptor.decrypt(any(), any()))
        .thenThrow(IllegalArgumentException("Something went wrong"))

      async(CoroutineName(ATTEMPT_KEY) + Dispatchers.Default) {
          val exception =
            assertFailsWith(IllegalArgumentException::class) {
              CryptorExchangeTask.forDecryption(deterministicCommutativeCryptor)
                .execute(
                  mapOf(
                    "encryption-key" to KEY,
                    "encrypted-data" to makeSerializedSharedInputs(CIPHERTEXTS)
                  )
                )
            }
          assertThat(exception.message).contains("Something went wrong")
        }
        .await()
    }
  }

  @Test
  fun `decrypt with missing inputs`() {
    runBlocking {
      async(CoroutineName(ATTEMPT_KEY) + Dispatchers.Default) {
          assertFailsWith(IllegalArgumentException::class) {
            CryptorExchangeTask.forDecryption(deterministicCommutativeCryptor)
              .execute(mapOf("encrypted-data" to makeSerializedSharedInputs(CIPHERTEXTS)))
          }
          assertFailsWith(IllegalArgumentException::class) {
            CryptorExchangeTask.forDecryption(deterministicCommutativeCryptor)
              .execute(mapOf("encryption-key" to KEY))
          }
          verifyZeroInteractions(deterministicCommutativeCryptor)
        }
        .await()
    }
  }

  @Test
  fun `encrypt with valid inputs`() {
    runBlocking {
      whenever(deterministicCommutativeCryptor.encrypt(any(), any())).thenReturn(CIPHERTEXTS)

      async(CoroutineName(ATTEMPT_KEY) + Dispatchers.Default) {
          val result =
            CryptorExchangeTask.forEncryption(deterministicCommutativeCryptor)
              .execute(
                mapOf(
                  "encryption-key" to KEY,
                  "unencrypted-data" to makeSerializedSharedInputs(PLAINTEXTS)
                )
              )
          assertThat(result)
            .containsExactly("encrypted-data", makeSerializedSharedInputs(CIPHERTEXTS))
        }
        .await()
    }
  }

  @Test
  fun `encrypt with crypto error`() {
    runBlocking {
      whenever(deterministicCommutativeCryptor.encrypt(any(), any()))
        .thenThrow(IllegalArgumentException("Something went wrong"))

      async(CoroutineName(ATTEMPT_KEY) + Dispatchers.Default) {
          val exception =
            assertFailsWith(IllegalArgumentException::class) {
              CryptorExchangeTask.forEncryption(deterministicCommutativeCryptor)
                .execute(
                  mapOf(
                    "encryption-key" to KEY,
                    "unencrypted-data" to makeSerializedSharedInputs(PLAINTEXTS)
                  )
                )
            }
          assertThat(exception.message).contains("Something went wrong")
        }
        .await()
    }
  }

  @Test
  fun `encrypt with missing inputs`() {
    runBlocking {
      val job =
        async(CoroutineName(ATTEMPT_KEY) + Dispatchers.Default) {
          assertFailsWith(IllegalArgumentException::class) {
            CryptorExchangeTask.forEncryption(deterministicCommutativeCryptor)
              .execute(mapOf("unencrypted-data" to makeSerializedSharedInputs(PLAINTEXTS)))
          }
          assertFailsWith(IllegalArgumentException::class) {
            CryptorExchangeTask.forEncryption(deterministicCommutativeCryptor)
              .execute(mapOf("encryption-key" to KEY))
          }
          verifyZeroInteractions(deterministicCommutativeCryptor)
        }
      job.await()
    }
  }

  @Test
  fun `reEncryptTask with valid inputs`() {
    runBlocking {
      whenever(deterministicCommutativeCryptor.reEncrypt(any(), any()))
        .thenReturn(DOUBLE_CIPHERTEXTS)

      async(CoroutineName(ATTEMPT_KEY) + Dispatchers.Default) {
          val result =
            CryptorExchangeTask.forReEncryption(deterministicCommutativeCryptor)
              .execute(
                mapOf(
                  "encryption-key" to KEY,
                  "encrypted-data" to makeSerializedSharedInputs(CIPHERTEXTS)
                )
              )
          assertThat(result)
            .containsExactly("reencrypted-data", makeSerializedSharedInputs(DOUBLE_CIPHERTEXTS))
        }
        .await()
    }
  }

  @Test
  fun `reEncryptTask with crypto error`() {
    runBlocking {
      whenever(deterministicCommutativeCryptor.reEncrypt(any(), any()))
        .thenThrow(IllegalArgumentException("Something went wrong"))

      async(CoroutineName(ATTEMPT_KEY) + Dispatchers.Default) {
          val exception =
            assertFailsWith(IllegalArgumentException::class) {
              CryptorExchangeTask.forReEncryption(deterministicCommutativeCryptor)
                .execute(
                  mapOf(
                    "encryption-key" to KEY,
                    "encrypted-data" to makeSerializedSharedInputs(CIPHERTEXTS)
                  )
                )
            }
          assertThat(exception.message).contains("Something went wrong")
        }
        .await()
    }
  }

  @Test
  fun `reEncryptTask with missing inputs`() {
    runBlocking {
      async(CoroutineName(ATTEMPT_KEY) + Dispatchers.Default) {
          assertFailsWith(IllegalArgumentException::class) {
            CryptorExchangeTask.forReEncryption(deterministicCommutativeCryptor)
              .execute(mapOf("encrypted-data" to makeSerializedSharedInputs(CIPHERTEXTS)))
          }
          assertFailsWith(IllegalArgumentException::class) {
            CryptorExchangeTask.forReEncryption(deterministicCommutativeCryptor)
              .execute(mapOf("encryption-key" to KEY))
          }
          verifyZeroInteractions(deterministicCommutativeCryptor)
        }
        .await()
    }
  }
}
