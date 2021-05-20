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
import org.junit.Test
import org.wfanet.panelmatch.protocol.common.CommutativeEncryption

/** Abstract base class for testing implementations of [CommutativeEncryption]. */
abstract class AbstractCommutativeEncryptionTest {
  abstract val commutativeEncryption: CommutativeEncryption

  @Test
  fun testCommutativeEncryption() {
    val plaintexts =
      listOf<ByteString>(
        ByteString.copyFromUtf8("some plaintext0"),
        ByteString.copyFromUtf8("some plaintext1"),
        ByteString.copyFromUtf8("some plaintext2"),
        ByteString.copyFromUtf8("some plaintext3"),
        ByteString.copyFromUtf8("some plaintext4")
      )
    val randomKey1: ByteString = ByteString.copyFromUtf8("random-key-00")
    val randomKey2: ByteString = ByteString.copyFromUtf8("random-key-222")

    val encryptedTexts1 = commutativeEncryption.encrypt(randomKey1, plaintexts)
    val encryptedTexts2 = commutativeEncryption.encrypt(randomKey2, plaintexts)
    assertThat(encryptedTexts1).isNotEqualTo(encryptedTexts2)

    val reEncryptedTexts1 = commutativeEncryption.reencrypt(randomKey1, encryptedTexts2)
    val reEncryptedTexts2 = commutativeEncryption.reencrypt(randomKey2, encryptedTexts1)
    assertThat(reEncryptedTexts1).isNotEqualTo(encryptedTexts2)
    assertThat(reEncryptedTexts2).isNotEqualTo(encryptedTexts1)

    val decryptedTexts1 = commutativeEncryption.decrypt(randomKey1, reEncryptedTexts1)
    val decryptedTexts2 = commutativeEncryption.decrypt(randomKey1, reEncryptedTexts2)
    assertThat(decryptedTexts1).isEqualTo(encryptedTexts2)
    assertThat(decryptedTexts2).isEqualTo(encryptedTexts2)

    val decryptedTexts3 = commutativeEncryption.decrypt(randomKey2, reEncryptedTexts1)
    val decryptedTexts4 = commutativeEncryption.decrypt(randomKey2, reEncryptedTexts2)
    assertThat(decryptedTexts3).isEqualTo(encryptedTexts1)
    assertThat(decryptedTexts4).isEqualTo(encryptedTexts1)
  }
}
