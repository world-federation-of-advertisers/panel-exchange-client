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
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.panelmatch.client.exchangetasks.ExchangeTaskMapper
import org.wfanet.panelmatch.client.storage.InMemoryStorage
import org.wfanet.panelmatch.client.storage.Storage
import org.wfanet.panelmatch.protocol.common.Cryptor
import org.wfanet.panelmatch.protocol.common.makeSerializedSharedInputs
import org.wfanet.panelmatch.protocol.common.parseSerializedSharedInputs

private val DP_0_SECRET_KEY = ByteString.copyFromUtf8("random-edp-string-0")
private val MP_0_SECRET_KEY = ByteString.copyFromUtf8("random-mp-string-0")
private val joinKeys =
  listOf<ByteString>(
    ByteString.copyFromUtf8("some joinkey0"),
    ByteString.copyFromUtf8("some joinkey1"),
    ByteString.copyFromUtf8("some joinkey2"),
    ByteString.copyFromUtf8("some joinkey3"),
    ByteString.copyFromUtf8("some joinkey4")
  )
private val singleBlindedKeys =
  listOf<ByteString>(
    ByteString.copyFromUtf8("some single-blinded key0"),
    ByteString.copyFromUtf8("some single-blinded key1"),
    ByteString.copyFromUtf8("some single-blinded key2"),
    ByteString.copyFromUtf8("some single-blinded key3"),
    ByteString.copyFromUtf8("some single-blinded key4")
  )
private val doubleBlindedKeys =
  listOf<ByteString>(
    ByteString.copyFromUtf8("some double-blinded key0"),
    ByteString.copyFromUtf8("some double-blinded key1"),
    ByteString.copyFromUtf8("some double-blinded key2"),
    ByteString.copyFromUtf8("some double-blinded key3"),
    ByteString.copyFromUtf8("some double-blinded key4")
  )
private val lookupKeys =
  listOf<ByteString>(
    ByteString.copyFromUtf8("some lookup0"),
    ByteString.copyFromUtf8("some lookup1"),
    ByteString.copyFromUtf8("some lookup2"),
    ByteString.copyFromUtf8("some lookup3"),
    ByteString.copyFromUtf8("some lookup4")
  )
private val deterministicCommutativeCryptor = mock<Cryptor>()

@RunWith(JUnit4::class)
class CoroutineLauncherTest {
  private class TestStep
  constructor(
    val inputLabels: Map<String, String>,
    val outputLabels: Map<String, String>,
    val stepType: ExchangeWorkflow.Step.StepCase,
    val encryptFormat: ExchangeWorkflow.Step.EncryptAndShareStep.InputFormat =
      ExchangeWorkflow.Step.EncryptAndShareStep.InputFormat.INPUT_FORMAT_UNSPECIFIED,
    val inputData: Map<String, ByteString> = emptyMap<String, ByteString>()
  ) {

    private suspend fun build(): ExchangeWorkflow.Step {
      var stepBuilder = ExchangeWorkflow.Step.newBuilder()
      stepBuilder =
        when (stepType) {
          ExchangeWorkflow.Step.StepCase.INPUT ->
            stepBuilder.apply { input = ExchangeWorkflow.Step.InputStep.getDefaultInstance() }
          ExchangeWorkflow.Step.StepCase.ENCRYPT_AND_SHARE ->
            stepBuilder.apply {
              encryptAndShare =
                ExchangeWorkflow.Step.EncryptAndShareStep.newBuilder()
                  .apply { inputFormat = encryptFormat }
                  .build()
            }
          ExchangeWorkflow.Step.StepCase.DECRYPT ->
            stepBuilder.apply { decrypt = ExchangeWorkflow.Step.DecryptStep.getDefaultInstance() }
          else -> throw Exception("Unsupported step config")
        }
      return stepBuilder.putAllInputLabels(inputLabels).putAllOutputLabels(outputLabels).build()
    }

    suspend fun buildAndExecute(storage: Storage): Map<String, ByteString> {
      val builtStep: ExchangeWorkflow.Step = build()
      whenever(deterministicCommutativeCryptor.encrypt(any(), any())).thenReturn(singleBlindedKeys)
      whenever(deterministicCommutativeCryptor.reEncrypt(any(), any()))
        .thenReturn(doubleBlindedKeys)
      whenever(deterministicCommutativeCryptor.decrypt(any(), any())).thenReturn(lookupKeys)
      return ExchangeTaskMapper(deterministicCommutativeCryptor)
        .execute(builtStep, inputData, storage)
    }
  }

  @Test
  fun `test single party initiating steps for both parties in shared memory`() = runBlocking {
    val storage = InMemoryStorage()
    val testSteps: List<TestStep> =
      listOf(
        TestStep(
          inputLabels = mapOf("input" to "encryption-key"),
          outputLabels = mapOf("output" to "mp-crypto-key"),
          stepType = ExchangeWorkflow.Step.StepCase.INPUT,
          inputData = mapOf("encryption-key" to MP_0_SECRET_KEY)
        ),
        TestStep(
          inputLabels = mapOf("input" to "encryption-key"),
          outputLabels = mapOf("output" to "dp-crypto-key"),
          stepType = ExchangeWorkflow.Step.StepCase.INPUT,
          inputData = mapOf("encryption-key" to DP_0_SECRET_KEY)
        ),
        TestStep(
          inputLabels = mapOf("input" to "joinkeys"),
          outputLabels = mapOf("output" to "mp-joinkeys"),
          stepType = ExchangeWorkflow.Step.StepCase.INPUT,
          inputData = mapOf("joinkeys" to makeSerializedSharedInputs(joinKeys))
        ),
        TestStep(
          inputLabels =
            mapOf("encryption-key" to "mp-crypto-key", "unencrypted-data" to "mp-joinkeys"),
          outputLabels = mapOf("encrypted-data" to "mp-single-blinded-joinkeys"),
          stepType = ExchangeWorkflow.Step.StepCase.ENCRYPT_AND_SHARE,
          encryptFormat = ExchangeWorkflow.Step.EncryptAndShareStep.InputFormat.PLAINTEXT
        ),
        TestStep(
          inputLabels =
            mapOf(
              "encryption-key" to "dp-crypto-key",
              "encrypted-data" to "mp-single-blinded-joinkeys"
            ),
          outputLabels = mapOf("reencrypted-data" to "dp-mp-double-blinded-joinkeys"),
          stepType = ExchangeWorkflow.Step.StepCase.ENCRYPT_AND_SHARE,
          encryptFormat = ExchangeWorkflow.Step.EncryptAndShareStep.InputFormat.CIPHERTEXT
        ),
        TestStep(
          inputLabels =
            mapOf(
              "encryption-key" to "mp-crypto-key",
              "encrypted-data" to "dp-mp-double-blinded-joinkeys"
            ),
          outputLabels = mapOf("decrypted-data" to "decrypted-data"),
          stepType = ExchangeWorkflow.Step.StepCase.DECRYPT
        )
      )
    val stepOutputs = mutableListOf<Map<String, ByteString>>()
    for (testStep in testSteps) {
      stepOutputs.add(testStep.buildAndExecute(storage))
    }
    // Verify single blinded output
    assertThat(parseSerializedSharedInputs(requireNotNull(stepOutputs[3]["encrypted-data"])))
      .isEqualTo(singleBlindedKeys)

    // Verify double blinded output
    assertThat(parseSerializedSharedInputs(requireNotNull(stepOutputs[4]["reencrypted-data"])))
      .isEqualTo(doubleBlindedKeys)

    // Verify decrypted double blinded output
    assertThat(parseSerializedSharedInputs(requireNotNull(stepOutputs[5]["decrypted-data"])))
      .isEqualTo(lookupKeys)
  }
}
