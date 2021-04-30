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

package wfanet.panelmatch.protocol.common

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import wfanet.panelmatch.protocol.protobuf.ApplyCommutativeDecryptionRequest
import wfanet.panelmatch.protocol.protobuf.ApplyCommutativeDecryptionResponse
import wfanet.panelmatch.protocol.protobuf.ApplyCommutativeEncryptionRequest
import wfanet.panelmatch.protocol.protobuf.ApplyCommutativeEncryptionResponse
import wfanet.panelmatch.protocol.protobuf.ReApplyCommutativeEncryptionRequest
import wfanet.panelmatch.protocol.protobuf.ReApplyCommutativeEncryptionResponse

@RunWith(JUnit4::class)
class CommutativeEncryptionUtilityTest {

    @Test
    fun testCommutativeEncryption() {
        val plaintexts = listOf<ByteString>(
            ByteString.copyFromUtf8("some plaintext0"),
            ByteString.copyFromUtf8("some plaintext1"),
            ByteString.copyFromUtf8("some plaintext2"),
            ByteString.copyFromUtf8("some plaintext3"),
            ByteString.copyFromUtf8("some plaintext4")
        )
        val randomKey1: ByteString = ByteString.copyFromUtf8("random-key-00")
        val randomKey2: ByteString = ByteString.copyFromUtf8("asdfrasdfandom-key-222")

        val encryptRequest1 =
            ApplyCommutativeEncryptionRequest
                .newBuilder()
                .setEncryptionKey(randomKey1)
                .addAllPlaintexts(plaintexts)
                .build()
        val encryptedResponse1: ApplyCommutativeEncryptionResponse =
            JniCommutativeEncryption().applyCommutativeEncryption(encryptRequest1)
        val encryptedTexts1 = encryptedResponse1.getEncryptedTextsList()

        val encryptRequest2 =
            ApplyCommutativeEncryptionRequest
                .newBuilder()
                .setEncryptionKey(randomKey2)
                .addAllPlaintexts(plaintexts)
                .build()
        val encryptedResponse2: ApplyCommutativeEncryptionResponse =
            JniCommutativeEncryption().applyCommutativeEncryption(encryptRequest2)
        val encryptedTexts2 = encryptedResponse2.getEncryptedTextsList()

        assertThat(encryptedTexts1).isNotEqualTo(encryptedTexts2)

        val reEncryptRequest1 =
            ReApplyCommutativeEncryptionRequest
                .newBuilder()
                .setEncryptionKey(randomKey1)
                .addAllEncryptedTexts(encryptedTexts2)
                .build()
        val reEncryptedResponse1: ReApplyCommutativeEncryptionResponse =
            JniCommutativeEncryption().reApplyCommutativeEncryption(reEncryptRequest1)
        val reEncryptedTexts1 = reEncryptedResponse1.getReencryptedTextsList()

        assertThat(reEncryptedTexts1).isNotEqualTo(encryptedTexts2)

        val reEncryptRequest2 =
            ReApplyCommutativeEncryptionRequest
                .newBuilder()
                .setEncryptionKey(randomKey2)
                .addAllEncryptedTexts(encryptedTexts1)
                .build()
        val reEncryptedResponse2: ReApplyCommutativeEncryptionResponse =
            JniCommutativeEncryption().reApplyCommutativeEncryption(reEncryptRequest2)
        val reEncryptedTexts2 = reEncryptedResponse2.getReencryptedTextsList()

        assertThat(reEncryptedTexts2).isNotEqualTo(encryptedTexts1)

        val decryptRequest1 =
            ApplyCommutativeDecryptionRequest
                .newBuilder()
                .setEncryptionKey(randomKey1)
                .addAllEncryptedTexts(reEncryptedTexts1)
                .build()
        val decryptedResponse1: ApplyCommutativeDecryptionResponse =
            JniCommutativeEncryption().applyCommutativeDecryption(decryptRequest1)
        val decryptedTexts1 = decryptedResponse1.getDecryptedTextsList()

        assertThat(decryptedTexts1).isEqualTo(encryptedTexts2)

        val decryptRequest2 =
            ApplyCommutativeDecryptionRequest
                .newBuilder()
                .setEncryptionKey(randomKey1)
                .addAllEncryptedTexts(reEncryptedTexts2)
                .build()
        val decryptedResponse2: ApplyCommutativeDecryptionResponse =
            JniCommutativeEncryption().applyCommutativeDecryption(decryptRequest2)
        val decryptedTexts2 = decryptedResponse2.getDecryptedTextsList()

        assertThat(decryptedTexts2).isEqualTo(encryptedTexts2)

        val decryptRequest3 =
            ApplyCommutativeDecryptionRequest
                .newBuilder()
                .setEncryptionKey(randomKey2)
                .addAllEncryptedTexts(reEncryptedTexts1)
                .build()
        val decryptedResponse3: ApplyCommutativeDecryptionResponse =
            JniCommutativeEncryption().applyCommutativeDecryption(decryptRequest3)
        val decryptedTexts3 = decryptedResponse3.getDecryptedTextsList()

        assertThat(decryptedTexts3).isEqualTo(encryptedTexts1)

        val decryptRequest4 =
            ApplyCommutativeDecryptionRequest
                .newBuilder()
                .setEncryptionKey(randomKey2)
                .addAllEncryptedTexts(reEncryptedTexts2)
                .build()
        val decryptedResponse4: ApplyCommutativeDecryptionResponse =
            JniCommutativeEncryption().applyCommutativeDecryption(decryptRequest4)
        val decryptedTexts4 = decryptedResponse4.getDecryptedTextsList()

        assertThat(decryptedTexts4).isEqualTo(encryptedTexts1)
    }
}
