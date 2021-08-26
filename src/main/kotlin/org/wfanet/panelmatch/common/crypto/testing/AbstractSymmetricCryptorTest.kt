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

package org.wfanet.panelmatch.common.crypto.testing

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.wfanet.panelmatch.common.crypto.SymmetricCryptor

private val PLAINTEXT = ByteString.copyFromUtf8("some-long-long-plaintext")
private val PRIVATE_KEY1 = ByteString.copyFromUtf8("some-private-key")
private val PRIVATE_KEY2 = ByteString.copyFromUtf8("some-other-private-key")

abstract class AbstractSymmetricCryptorTest() {
  abstract val symmetricCryptor: SymmetricCryptor

  @Test
  fun `encrypt result should not equal original data`() {
    assertThat(symmetricCryptor.encrypt(PRIVATE_KEY1, PLAINTEXT)).isNotEqualTo(PLAINTEXT)
  }

  @Test
  fun `encrypt data and then decrypt result should equal original data`() = runBlocking {
    val encryptedValue = symmetricCryptor.encrypt(PRIVATE_KEY1, PLAINTEXT)
    val decryptedValue = symmetricCryptor.decrypt(PRIVATE_KEY1, encryptedValue)
    assertThat(decryptedValue).isEqualTo(PLAINTEXT)
  }

  @Test
  fun `encrypt data with two different keys should not be equal`() = runBlocking {
    val encryptedValue1 = symmetricCryptor.encrypt(PRIVATE_KEY1, PLAINTEXT)
    val encryptedValue2 = symmetricCryptor.encrypt(PRIVATE_KEY2, PLAINTEXT)
    assertThat(encryptedValue1).isNotEqualTo(encryptedValue2)
  }
}
