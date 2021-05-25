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

package org.wfanet.panelmatch.client.launcher

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.measurement.api.v2alpha.ExchangeStep
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttempt
import org.wfanet.panelmatch.client.storage.InMemoryStorage
import org.wfanet.panelmatch.client.storage.Storage
import org.wfanet.panelmatch.protocol.common.applyCommutativeEncryption
import org.wfanet.panelmatch.protocol.common.makeSerializedSharedInputs
import org.wfanet.panelmatch.protocol.common.parseSerializedSharedInputs
import org.wfanet.panelmatch.protocol.common.reApplyCommutativeEncryption

@RunWith(JUnit4::class)
class CoroutineLauncherTest {
  private val DP_0_SECRET_KEY = ByteString.copyFromUtf8("random-edp-string-0")
  private val MP_0_SECRET_KEY = ByteString.copyFromUtf8("random-mp-string-0")
  private val joinkeys =
    listOf<ByteString>(
      ByteString.copyFromUtf8("some joinkey0"),
      ByteString.copyFromUtf8("some joinkey1"),
      ByteString.copyFromUtf8("some joinkey2"),
      ByteString.copyFromUtf8("some joinkey3"),
      ByteString.copyFromUtf8("some joinkey4")
    )
  private val apiClient: ApiClient = mock()

  private suspend fun executeStepConfig(
    stepConfig: Map<String, Any>,
    storage: Storage
  ): ByteString? {
    var exchangeStep = ExchangeStep.newBuilder()
    var step = ExchangeWorkflow.Step.newBuilder()
    val inputLabels = requireNotNull(stepConfig["inputLabels"]) as Map<String, String>
    val outputLabels = requireNotNull(stepConfig["outputLabels"]) as Map<String, String>
    step =
      when (stepConfig["stepType"]) {
        ExchangeWorkflow.Step.StepCase.INPUT ->
          step.apply { input = ExchangeWorkflow.Step.InputStep.getDefaultInstance() }
        ExchangeWorkflow.Step.StepCase.ENCRYPT_AND_SHARE ->
          step.apply {
            encryptAndShare =
              ExchangeWorkflow.Step.EncryptAndShareStep.newBuilder()
                .apply {
                  inputFormat =
                    stepConfig["inputFormat"] as
                      ExchangeWorkflow.Step.EncryptAndShareStep.InputFormat
                }
                .build()
          }
        ExchangeWorkflow.Step.StepCase.DECRYPT ->
          step.apply { decrypt = ExchangeWorkflow.Step.DecryptStep.getDefaultInstance() }
        else -> throw Exception("Unsupported step config")
      }
    val builtExchangeStep = exchangeStep.apply{
      step =  step.putAllInputLabels(inputLabels).putAllOutputLabels(outputLabels)
    }.build()
    CoroutineLauncher()
        .execute(
          apiClient,
          builtExchangeStep,
          ExchangeStepAttempt.Key.getDefaultInstance()
          //stepConfig["inputData"] as Map<String, ByteString>,
          //storage
        )
    println("********* WAITING *********")
    //whenever(apiClient.finishExchangeStepAttempt(any(), any(), any())).thenReturn(ByteString.copyFromUtf8("some joinkey0"))
    return ByteString.copyFromUtf8("some joinkey0")
  }
  @Test
  fun `test single party initiating steps for both parties in shared memory`() = runBlocking {
    val storage = InMemoryStorage()
    val stepsConfig =
      listOf(
        mapOf(
          "inputLabels" to mapOf("input" to "crypto-key"),
          "outputLabels" to mapOf("output" to "mp-crypto-key"),
          "stepType" to ExchangeWorkflow.Step.StepCase.INPUT,
          "inputData" to mapOf("crypto-key" to MP_0_SECRET_KEY)
        ),
        mapOf(
          "inputLabels" to mapOf("input" to "crypto-key"),
          "outputLabels" to mapOf("output" to "dp-crypto-key"),
          "stepType" to ExchangeWorkflow.Step.StepCase.INPUT,
          "inputData" to mapOf("crypto-key" to DP_0_SECRET_KEY)
        ),
        mapOf(
          "inputLabels" to mapOf("input" to "joinkeys"),
          "outputLabels" to mapOf("output" to "mp-joinkeys"),
          "stepType" to ExchangeWorkflow.Step.StepCase.INPUT,
          "inputData" to mapOf("joinkeys" to makeSerializedSharedInputs(joinkeys))
        ),
        mapOf(
          "inputLabels" to
            mapOf("crypto-key" to "mp-crypto-key", "unencrypted-data" to "mp-joinkeys"),
          "outputLabels" to mapOf("output" to "mp-single-blinded-joinkeys"),
          "stepType" to ExchangeWorkflow.Step.StepCase.ENCRYPT_AND_SHARE,
          "inputFormat" to ExchangeWorkflow.Step.EncryptAndShareStep.InputFormat.PLAINTEXT,
          "inputData" to emptyMap<String, ByteString>()
        ),
        mapOf(
          "inputLabels" to
            mapOf(
              "crypto-key" to "dp-crypto-key",
              "encrypted-data" to "mp-single-blinded-joinkeys"
            ),
          "outputLabels" to mapOf("output" to "dp-mp-double-blinded-joinkeys"),
          "stepType" to ExchangeWorkflow.Step.StepCase.ENCRYPT_AND_SHARE,
          "inputFormat" to ExchangeWorkflow.Step.EncryptAndShareStep.InputFormat.CIPHERTEXT,
          "inputData" to emptyMap<String, ByteString>()
        ),
        mapOf(
          "inputLabels" to
            mapOf(
              "crypto-key" to "mp-crypto-key",
              "encrypted-data" to "dp-mp-double-blinded-joinkeys"
            ),
          "outputLabels" to mapOf("output" to "decrypted-data"),
          "stepType" to ExchangeWorkflow.Step.StepCase.DECRYPT,
          "inputData" to emptyMap<String, ByteString>()
        )
      )
    val stepOutputs = mutableListOf<ByteString?>()
    for (stepConfig in stepsConfig) {
      stepOutputs.add(executeStepConfig(stepConfig, storage))
    }

    // Verify single blinded output
    val encryptedJoinKeys = applyCommutativeEncryption(MP_0_SECRET_KEY, joinkeys)
    assertThat(encryptedJoinKeys)
      .isEqualTo(parseSerializedSharedInputs(requireNotNull(stepOutputs[3])))

    // Verify double blinded output
    val reEncryptedJoinKeys = reApplyCommutativeEncryption(DP_0_SECRET_KEY, encryptedJoinKeys)
    assertThat(reEncryptedJoinKeys)
      .isEqualTo(parseSerializedSharedInputs(requireNotNull(stepOutputs[4])))

    // Verify decrypted double blinded output
    val decryptedJoinKeys = reApplyCommutativeEncryption(MP_0_SECRET_KEY, reEncryptedJoinKeys)
    assertThat(decryptedJoinKeys)
      .isEqualTo(parseSerializedSharedInputs(requireNotNull(stepOutputs[5])))
  }
}
