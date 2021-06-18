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

package org.wfanet.panelmatch.client.launcher.testing

import com.google.protobuf.ByteString
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import java.time.Duration
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.wfanet.measurement.api.v2alpha.ExchangeStep
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttempt
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.panelmatch.client.launcher.ApiClient
import org.wfanet.panelmatch.client.launcher.CoroutineLauncher
import org.wfanet.panelmatch.client.launcher.ExchangeTaskMapper
import org.wfanet.panelmatch.client.storage.Storage
import org.wfanet.panelmatch.protocol.common.Cryptor

val DP_0_SECRET_KEY = ByteString.copyFromUtf8("random-edp-string-0")
val MP_0_SECRET_KEY = ByteString.copyFromUtf8("random-mp-string-0")
val JOIN_KEYS =
  listOf<ByteString>(
    ByteString.copyFromUtf8("some joinkey0"),
    ByteString.copyFromUtf8("some joinkey1"),
    ByteString.copyFromUtf8("some joinkey2"),
    ByteString.copyFromUtf8("some joinkey3"),
    ByteString.copyFromUtf8("some joinkey4")
  )
val SINGLE_BLINDED_KEYS =
  listOf<ByteString>(
    ByteString.copyFromUtf8("some single-blinded key0"),
    ByteString.copyFromUtf8("some single-blinded key1"),
    ByteString.copyFromUtf8("some single-blinded key2"),
    ByteString.copyFromUtf8("some single-blinded key3"),
    ByteString.copyFromUtf8("some single-blinded key4")
  )
val DOUBLE_BLINDED_KEYS =
  listOf<ByteString>(
    ByteString.copyFromUtf8("some double-blinded key0"),
    ByteString.copyFromUtf8("some double-blinded key1"),
    ByteString.copyFromUtf8("some double-blinded key2"),
    ByteString.copyFromUtf8("some double-blinded key3"),
    ByteString.copyFromUtf8("some double-blinded key4")
  )
val LOOKUP_KEYS =
  listOf<ByteString>(
    ByteString.copyFromUtf8("some lookup0"),
    ByteString.copyFromUtf8("some lookup1"),
    ByteString.copyFromUtf8("some lookup2"),
    ByteString.copyFromUtf8("some lookup3"),
    ByteString.copyFromUtf8("some lookup4")
  )

class TestStep(
  val apiClient: ApiClient,
  val exchangeKey: String,
  val exchangeStepAttemptKey: String,
  val privateInputLabels: Map<String, String> = emptyMap<String, String>(),
  val privateOutputLabels: Map<String, String> = emptyMap<String, String>(),
  val sharedInputLabels: Map<String, String> = emptyMap<String, String>(),
  val sharedOutputLabels: Map<String, String> = emptyMap<String, String>(),
  val stepType: ExchangeWorkflow.Step.StepCase,
  val deterministicCommutativeCryptor: Cryptor = mock<Cryptor>(),
  val timeoutDuration: Duration = Duration.ofMillis(500),
  val retryDuration: Duration = Duration.ofMillis(100),
  val preferredSharedStorage: Storage,
  val preferredPrivateStorage: Storage,
  val attemptKey: ExchangeStepAttempt.Key =
    ExchangeStepAttempt.Key.newBuilder()
      .apply {
        exchangeId = exchangeKey
        exchangeStepAttemptId = exchangeStepAttemptKey
      }
      .build()
) {
  init {}
  suspend fun build(): ExchangeWorkflow.Step {
    return ExchangeWorkflow.Step.newBuilder()
      .putAllPrivateInputLabels(privateInputLabels)
      .putAllPrivateOutputLabels(privateOutputLabels)
      .putAllSharedInputLabels(sharedInputLabels)
      .putAllSharedOutputLabels(sharedOutputLabels)
      .apply {
        when (stepType) {
          ExchangeWorkflow.Step.StepCase.INPUT_STEP ->
            inputStep = ExchangeWorkflow.Step.InputStep.getDefaultInstance()
          ExchangeWorkflow.Step.StepCase.ENCRYPT_STEP ->
            encryptStep = ExchangeWorkflow.Step.EncryptStep.getDefaultInstance()
          ExchangeWorkflow.Step.StepCase.REENCRYPT_STEP ->
            reencryptStep = ExchangeWorkflow.Step.ReEncryptStep.getDefaultInstance()
          ExchangeWorkflow.Step.StepCase.DECRYPT_STEP ->
            decryptStep = ExchangeWorkflow.Step.DecryptStep.getDefaultInstance()
          else -> error("Unsupported step config")
        }
      }
      .build()
  }

  suspend fun buildAndExecute() = runBlocking {
    val builtStep: ExchangeWorkflow.Step = build()
    whenever(deterministicCommutativeCryptor.encrypt(any(), any())).thenReturn(SINGLE_BLINDED_KEYS)
    whenever(deterministicCommutativeCryptor.reEncrypt(any(), any()))
      .thenReturn(DOUBLE_BLINDED_KEYS)
    whenever(deterministicCommutativeCryptor.decrypt(any(), any())).thenReturn(LOOKUP_KEYS)
    val job =
      async(CoroutineName(attemptKey.exchangeId) + Dispatchers.Default) {
        ExchangeTaskMapper(
            apiClient = apiClient,
            preferredSharedStorage = preferredSharedStorage,
            preferredPrivateStorage = preferredPrivateStorage,
            deterministicCommutativeCryptor = deterministicCommutativeCryptor,
            timeoutDuration = timeoutDuration,
            retryDuration = retryDuration
          )
          .execute(attemptKey = attemptKey, step = builtStep)
      }
    job.await()
  }

  suspend fun buildAndExecuteJob() {
    val builtStep: ExchangeWorkflow.Step = build()
    val exchangeStep =
      ExchangeStep.newBuilder()
        .apply {
          keyBuilder.apply { step = builtStep }
          state = ExchangeStep.State.READY_FOR_RETRY
        }
        .build()
    whenever(deterministicCommutativeCryptor.encrypt(any(), any())).thenReturn(SINGLE_BLINDED_KEYS)
    whenever(deterministicCommutativeCryptor.reEncrypt(any(), any()))
      .thenReturn(DOUBLE_BLINDED_KEYS)
    whenever(deterministicCommutativeCryptor.decrypt(any(), any())).thenReturn(LOOKUP_KEYS)
    CoroutineLauncher(
        apiClient = apiClient,
        preferredSharedStorage = preferredSharedStorage,
        preferredPrivateStorage = preferredPrivateStorage,
        deterministicCommutativeCryptor = deterministicCommutativeCryptor
      )
      .execute(exchangeStep = exchangeStep, attemptKey = attemptKey)
  }
}
