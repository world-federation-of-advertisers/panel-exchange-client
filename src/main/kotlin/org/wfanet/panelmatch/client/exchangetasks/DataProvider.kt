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

package org.wfanet.panelmatch.client.exchangetasks

import com.google.protobuf.ByteString
import org.wfanet.panelmatch.protocol.common.JniCommutativeEncryption
import wfanet.panelmatch.protocol.protobuf.ApplyCommutativeDecryptionRequest
import wfanet.panelmatch.protocol.protobuf.ApplyCommutativeEncryptionRequest
import wfanet.panelmatch.protocol.protobuf.ReApplyCommutativeEncryptionRequest

class DataProvider {

  enum class PanelProviderAction {
    INTERSECT_AND_VALIDATE,
    ENCRYPT_AND_SHARE,
  }

  fun reApplyCommutativeEncryption(
    dataProviderPrivateKey: ByteString,
    encryptedTexts: List<ByteString>
  ): List<ByteString> {
    val request =
      ReApplyCommutativeEncryptionRequest.newBuilder()
        .setEncryptionKey(dataProviderPrivateKey)
        .addAllEncryptedTexts(encryptedTexts)
        .build()
    return JniCommutativeEncryption()
      .reApplyCommutativeEncryption(request)
      .getReencryptedTextsList()
  }

  fun intersectAndValidate(
    encryptedTexts: List<ByteString>
  ): List<ByteString> {
    return error("Not implemented")
  }

}
