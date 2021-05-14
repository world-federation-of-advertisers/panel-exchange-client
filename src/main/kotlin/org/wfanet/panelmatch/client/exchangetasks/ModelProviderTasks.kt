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
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.panelmatch.protocol.common.JniCommutativeEncryption
import wfanet.panelmatch.protocol.protobuf.ApplyCommutativeDecryptionRequest
import wfanet.panelmatch.protocol.protobuf.ApplyCommutativeEncryptionRequest
import wfanet.panelmatch.protocol.protobuf.SharedInputs

class ModelProviderTasks : ExchangeTask {

  private fun encryptJoinKeys(key: ByteString, plaintexts: List<ByteString>): List<ByteString> {
    val request =
      ApplyCommutativeEncryptionRequest.newBuilder()
        .setEncryptionKey(key)
        .addAllPlaintexts(plaintexts)
        .build()
    return JniCommutativeEncryption().applyCommutativeEncryption(request).getEncryptedTextsList()
  }

  private fun decryptJoinKeys(key: ByteString, encryptedTexts: List<ByteString>): List<ByteString> {
    val request =
      ApplyCommutativeDecryptionRequest.newBuilder()
        .setEncryptionKey(key)
        .addAllEncryptedTexts(encryptedTexts)
        .build()
    return JniCommutativeEncryption().applyCommutativeDecryption(request).getDecryptedTextsList()
  }

  override suspend fun execute(
    step: ExchangeWorkflow.Step,
    input: Map<String, ByteString>
  ): Map<String, ByteString> {
    when (step.getStepCase()) {
      ExchangeWorkflow.Step.StepCase.ENCRYPT_AND_SHARE ->
        return mapOf(
          "result" to
            SharedInputs.newBuilder()
              .addAllData(
                encryptJoinKeys(
                  input["crypto-key"]!!,
                  SharedInputs.parseFrom(input["data"]).getDataList()
                )
              )
              .build()
              .toByteString()
        )
      ExchangeWorkflow.Step.StepCase.DECRYPT ->
        return mapOf(
          "result" to
            SharedInputs.newBuilder()
              .addAllData(
                decryptJoinKeys(
                  input["crypto-key"]!!,
                  SharedInputs.parseFrom(input["data"]).getDataList()
                )
              )
              .build()
              .toByteString()
        )
      else -> {
        error("Unsupported step for Model Provider")
      }
    }
  }
}
