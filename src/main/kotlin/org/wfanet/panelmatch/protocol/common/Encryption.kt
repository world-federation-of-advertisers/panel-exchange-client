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

package org.wfanet.panelmatch.protocol.common

import com.google.protobuf.ByteString
import wfanet.panelmatch.protocol.protobuf.ApplyDecryptionRequest
import wfanet.panelmatch.protocol.protobuf.ApplyDecryptionResponse
import wfanet.panelmatch.protocol.protobuf.ApplyEncryptionRequest
import wfanet.panelmatch.protocol.protobuf.ApplyEncryptionResponse
import wfanet.panelmatch.protocol.protobuf.ReApplyEncryptionRequest
import wfanet.panelmatch.protocol.protobuf.ReApplyEncryptionResponse

/** Core deterministic, commutative cryptographic operations. */
interface DeterministicCommutativeEncryption {

  /** Encrypts plaintexts. */
  fun encrypt(request: ApplyEncryptionRequest): ApplyEncryptionResponse

  /** Adds an additional layer of encryption to ciphertexts. */
  fun reEncrypt(request: ReApplyEncryptionRequest): ReApplyEncryptionResponse

  /** Removes a layer of encryption from ciphertexts. */
  fun decrypt(request: ApplyDecryptionRequest): ApplyDecryptionResponse

  /** Encrypts plaintexts. */
  fun encrypt(key: ByteString, plaintexts: List<ByteString>): List<ByteString> {
    val request =
      ApplyEncryptionRequest.newBuilder().setEncryptionKey(key).addAllPlaintexts(plaintexts).build()
    return encrypt(request).encryptedTextsList
  }

  /** Adds an additional layer of encryption to ciphertexts. */
  fun reEncrypt(key: ByteString, encryptedTexts: List<ByteString>): List<ByteString> {
    val request =
      ReApplyEncryptionRequest.newBuilder()
        .setEncryptionKey(key)
        .addAllEncryptedTexts(encryptedTexts)
        .build()
    return reEncrypt(request).reencryptedTextsList
  }

  /** Encrypts plaintexts. */
  fun decrypt(key: ByteString, encryptedTexts: List<ByteString>): List<ByteString> {
    val request =
      ApplyDecryptionRequest.newBuilder()
        .setEncryptionKey(key)
        .addAllEncryptedTexts(encryptedTexts)
        .build()
    return decrypt(request).decryptedTextsList
  }
}
