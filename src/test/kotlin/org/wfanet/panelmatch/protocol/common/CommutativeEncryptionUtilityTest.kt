// Copyright 2020 The Cross-Media Measurement Authors
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

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CommutativeEncryptionUtilityTest {

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
    val randomKey1: ByteString = ByteString.copyFromUtf8("random-key-000")
    val randomKey2: ByteString = ByteString.copyFromUtf8("random-key-222")

    val encryptedTexts1 = applyCommutativeEncryptionHelper(randomKey1, plaintexts)

    val encryptedTexts2 = applyCommutativeEncryptionHelper(randomKey2, plaintexts)

    assertThat(encryptedTexts1).isNotEqualTo(encryptedTexts2)

    val reEncryptedTexts1 = reApplyCommutativeEncryptionHelper(randomKey1, encryptedTexts2)

    assertThat(reEncryptedTexts1).isNotEqualTo(encryptedTexts2)

    val reEncryptedTexts2 = reApplyCommutativeEncryptionHelper(randomKey2, encryptedTexts1)

    assertThat(reEncryptedTexts2).isNotEqualTo(encryptedTexts1)

    val decryptedTexts1 = applyCommutativeDecryptionHelper(randomKey1, reEncryptedTexts1)

    assertThat(decryptedTexts1).isEqualTo(encryptedTexts2)

    val decryptedTexts2 = applyCommutativeDecryptionHelper(randomKey1, reEncryptedTexts2)

    assertThat(decryptedTexts2).isEqualTo(encryptedTexts2)

    val decryptedTexts3 = applyCommutativeDecryptionHelper(randomKey2, reEncryptedTexts1)

    assertThat(decryptedTexts3).isEqualTo(encryptedTexts1)

    val decryptedTexts4 = applyCommutativeDecryptionHelper(randomKey2, reEncryptedTexts2)

    assertThat(decryptedTexts4).isEqualTo(encryptedTexts1)
  }
}
