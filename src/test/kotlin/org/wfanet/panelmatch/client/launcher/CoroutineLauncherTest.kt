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
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlin.test.assertFailsWith
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttempt
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.panelmatch.client.launcher.testing.DP_0_SECRET_KEY
import org.wfanet.panelmatch.client.launcher.testing.JOIN_KEYS
import org.wfanet.panelmatch.client.launcher.testing.MP_0_SECRET_KEY
import org.wfanet.panelmatch.client.launcher.testing.SINGLE_BLINDED_KEYS
import org.wfanet.panelmatch.client.launcher.testing.TestStep
import org.wfanet.panelmatch.client.storage.InMemoryStorage
import org.wfanet.panelmatch.protocol.common.makeSerializedSharedInputs

@RunWith(JUnit4::class)
class CoroutineLauncherTest {
  private val apiClient: ApiClient = mock()
  private val privateStorage = InMemoryStorage(keyPrefix = "private")
  private val sharedStorage = InMemoryStorage(keyPrefix = "shared")

  @Test
  fun `test input task`() {
    val EXCHANGE_KEY = java.util.UUID.randomUUID().toString()
    val ATTEMPT_KEY = java.util.UUID.randomUUID().toString()
    runBlocking {
      coroutineScope {
        whenever(apiClient.finishExchangeStepAttempt(any(), any(), any())).thenReturn(Unit)
        val outputLabels = mapOf("output" to "$EXCHANGE_KEY-mp-crypto-key")
        val testStep =
          TestStep(
            apiClient = apiClient,
            exchangeKey = EXCHANGE_KEY,
            exchangeStepAttemptKey = ATTEMPT_KEY,
            privateOutputLabels = outputLabels,
            stepType = ExchangeWorkflow.Step.StepCase.INPUT_STEP,
            privateStorage = privateStorage,
            sharedStorage = sharedStorage
          )
        // Launch Job Asynchronously
        testStep.buildAndExecuteJob()
        // Model Provider asynchronously writes data
        launch {
          privateStorage.batchWrite(
            outputLabels = outputLabels,
            data = mapOf("output" to MP_0_SECRET_KEY)
          )
        }
        val waitStep =
          TestStep(
            apiClient = apiClient,
            exchangeKey = EXCHANGE_KEY,
            exchangeStepAttemptKey = ATTEMPT_KEY,
            privateInputLabels = mapOf("input" to "$EXCHANGE_KEY-encryption-key"),
            privateOutputLabels = outputLabels,
            stepType = ExchangeWorkflow.Step.StepCase.INPUT_STEP,
            privateStorage = privateStorage,
            sharedStorage = sharedStorage
          )
        // Wait for things to finish synchronously
        waitStep.buildAndExecuteTask()
        val readValues =
          requireNotNull(
            privateStorage.batchRead(inputLabels = mapOf("input" to "$EXCHANGE_KEY-mp-crypto-key"))[
              "input"]
          )
        assertThat(readValues).isEqualTo(MP_0_SECRET_KEY)
        verify(apiClient, times(0))
          .finishExchangeStepAttempt(any(), eq(ExchangeStepAttempt.State.FAILED), any())
        // There should be two successes. One for the initial job and another when we
        // wait
        verify(apiClient, times(2))
          .finishExchangeStepAttempt(any(), eq(ExchangeStepAttempt.State.SUCCEEDED), any())
      }
    }
  }

  @Test
  fun `test crypto task with private inputs and private output`() {
    val apiClient: ApiClient = mock()
    val EXCHANGE_KEY = java.util.UUID.randomUUID().toString()
    val ATTEMPT_KEY = java.util.UUID.randomUUID().toString()
    runBlocking {
      whenever(apiClient.finishExchangeStepAttempt(any(), any(), any())).thenReturn(Unit)
      val testStep =
        TestStep(
          apiClient = apiClient,
          exchangeKey = EXCHANGE_KEY,
          exchangeStepAttemptKey = ATTEMPT_KEY,
          privateInputLabels =
            mapOf(
              "encryption-key" to "$EXCHANGE_KEY-mp-crypto-key",
              "unencrypted-data" to "$EXCHANGE_KEY-mp-joinkeys"
            ),
          privateOutputLabels =
            mapOf("encrypted-data" to "$EXCHANGE_KEY-mp-single-blinded-joinkeys"),
          stepType = ExchangeWorkflow.Step.StepCase.ENCRYPT_STEP,
          privateStorage = privateStorage,
          sharedStorage = sharedStorage
        )
      privateStorage.batchWrite(
        outputLabels = mapOf("output" to "$EXCHANGE_KEY-mp-crypto-key"),
        data = mapOf("output" to MP_0_SECRET_KEY)
      )
      privateStorage.batchWrite(
        outputLabels = mapOf("output" to "$EXCHANGE_KEY-mp-joinkeys"),
        data = mapOf("output" to makeSerializedSharedInputs(JOIN_KEYS))
      )
      // Launch Job Asynchronously
      testStep.buildAndExecuteJob()
      val waitStep =
        TestStep(
          apiClient = apiClient,
          exchangeKey = EXCHANGE_KEY,
          exchangeStepAttemptKey = ATTEMPT_KEY,
          privateOutputLabels = mapOf("output" to "$EXCHANGE_KEY-mp-single-blinded-joinkeys"),
          stepType = ExchangeWorkflow.Step.StepCase.INPUT_STEP,
          privateStorage = privateStorage,
          sharedStorage = sharedStorage
        )
      // Wait for things to finish synchronously
      waitStep.buildAndExecuteTask()
      val readValues =
        requireNotNull(
          privateStorage.batchRead(
            inputLabels = mapOf("input" to "$EXCHANGE_KEY-mp-single-blinded-joinkeys")
          )["input"]
        )
      verify(apiClient, times(0))
        .finishExchangeStepAttempt(any(), eq(ExchangeStepAttempt.State.FAILED), any())
      // There should be two successes. One for the initial job and another when we wait
      verify(apiClient, times(2))
        .finishExchangeStepAttempt(any(), eq(ExchangeStepAttempt.State.SUCCEEDED), any())
    }
  }

  @Test
  fun `test crypto task with private and shared inputs and shared and private output`() {
    val apiClient: ApiClient = mock()
    val EXCHANGE_KEY = java.util.UUID.randomUUID().toString()
    val ATTEMPT_KEY = java.util.UUID.randomUUID().toString()
    runBlocking {
      whenever(apiClient.finishExchangeStepAttempt(any(), any(), any())).thenReturn(Unit)
      val testStep =
        TestStep(
          apiClient = apiClient,
          exchangeKey = EXCHANGE_KEY,
          exchangeStepAttemptKey = ATTEMPT_KEY,
          privateInputLabels = mapOf("encryption-key" to "$EXCHANGE_KEY-dp-crypto-key"),
          privateOutputLabels =
            mapOf("reencrypted-data" to "$EXCHANGE_KEY-dp-mp-double-blinded-joinkeys"),
          sharedInputLabels = mapOf("encrypted-data" to "$EXCHANGE_KEY-mp-single-blinded-joinkeys"),
          sharedOutputLabels =
            mapOf("reencrypted-data" to "$EXCHANGE_KEY-dp-mp-double-blinded-joinkeys"),
          stepType = ExchangeWorkflow.Step.StepCase.REENCRYPT_STEP,
          privateStorage = privateStorage,
          sharedStorage = sharedStorage
        )
      privateStorage.batchWrite(
        outputLabels = mapOf("output" to "$EXCHANGE_KEY-dp-crypto-key"),
        data = mapOf("output" to DP_0_SECRET_KEY)
      )
      sharedStorage.batchWrite(
        outputLabels = mapOf("output" to "$EXCHANGE_KEY-mp-single-blinded-joinkeys"),
        data = mapOf("output" to makeSerializedSharedInputs(SINGLE_BLINDED_KEYS))
      )
      // Launch Job Asynchronously
      testStep.buildAndExecuteJob()
      val waitStep =
        TestStep(
          apiClient = apiClient,
          exchangeKey = EXCHANGE_KEY,
          exchangeStepAttemptKey = ATTEMPT_KEY,
          privateOutputLabels = mapOf("output" to "$EXCHANGE_KEY-dp-mp-double-blinded-joinkeys"),
          sharedOutputLabels = mapOf("output" to "$EXCHANGE_KEY-dp-mp-double-blinded-joinkeys"),
          stepType = ExchangeWorkflow.Step.StepCase.INPUT_STEP,
          privateStorage = privateStorage,
          sharedStorage = sharedStorage
        )
      // Wait for things to finish synchronously
      waitStep.buildAndExecuteTask()
      val privateReadValues =
        requireNotNull(
          privateStorage.batchRead(
            inputLabels = mapOf("input" to "$EXCHANGE_KEY-dp-mp-double-blinded-joinkeys")
          )["input"]
        )
      val sharedReadValues =
        requireNotNull(
          sharedStorage.batchRead(
            inputLabels = mapOf("input" to "$EXCHANGE_KEY-dp-mp-double-blinded-joinkeys")
          )["input"]
        )
      verify(apiClient, times(0))
        .finishExchangeStepAttempt(any(), eq(ExchangeStepAttempt.State.FAILED), any())
      // There should be two successes. One for the initial job and another when we wait
      verify(apiClient, times(2))
        .finishExchangeStepAttempt(any(), eq(ExchangeStepAttempt.State.SUCCEEDED), any())
    }
  }

  @Test
  fun `test crypto task with missing shared inputs`() {
    val apiClient: ApiClient = mock()
    val EXCHANGE_KEY = java.util.UUID.randomUUID().toString()
    val ATTEMPT_KEY = java.util.UUID.randomUUID().toString()
    runBlocking {
      whenever(apiClient.finishExchangeStepAttempt(any(), any(), any())).thenReturn(Unit)
      val testStep =
        TestStep(
          apiClient = apiClient,
          exchangeKey = EXCHANGE_KEY,
          exchangeStepAttemptKey = ATTEMPT_KEY,
          privateInputLabels = mapOf("encryption-key" to "$EXCHANGE_KEY-dp-crypto-key"),
          privateOutputLabels =
            mapOf("reencrypted-data" to "$EXCHANGE_KEY-dp-mp-double-blinded-joinkeys"),
          sharedInputLabels = mapOf("encrypted-data" to "$EXCHANGE_KEY-mp-single-blinded-joinkeys"),
          sharedOutputLabels =
            mapOf("reencrypted-data" to "$EXCHANGE_KEY-dp-mp-double-blinded-joinkeys"),
          stepType = ExchangeWorkflow.Step.StepCase.REENCRYPT_STEP,
          privateStorage = privateStorage,
          sharedStorage = sharedStorage
        )
      privateStorage.batchWrite(
        outputLabels = mapOf("output" to "$EXCHANGE_KEY-dp-crypto-key"),
        data = mapOf("output" to DP_0_SECRET_KEY)
      )
      // Launch Job Asynchronously
      testStep.buildAndExecuteJob()
      val waitStep =
        TestStep(
          apiClient = apiClient,
          exchangeKey = EXCHANGE_KEY,
          exchangeStepAttemptKey = ATTEMPT_KEY,
          privateOutputLabels = mapOf("output" to "$EXCHANGE_KEY-dp-mp-double-blinded-joinkeys"),
          sharedOutputLabels = mapOf("output" to "$EXCHANGE_KEY-dp-mp-double-blinded-joinkeys"),
          stepType = ExchangeWorkflow.Step.StepCase.INPUT_STEP,
          privateStorage = privateStorage,
          sharedStorage = sharedStorage
        )
      val argumentException1 =
        assertFailsWith(TimeoutCancellationException::class) {
          // Wait for things to finish synchronously
          waitStep.buildAndExecuteTask()
        }
      // There should be two failures. One for the initial job and another when we wait
      verify(apiClient, times(2))
        .finishExchangeStepAttempt(any(), eq(ExchangeStepAttempt.State.FAILED), any())
      verify(apiClient, times(0))
        .finishExchangeStepAttempt(any(), eq(ExchangeStepAttempt.State.SUCCEEDED), any())
      val argumentException2 =
        assertFailsWith(IllegalArgumentException::class) {
          privateStorage.batchRead(
            inputLabels = mapOf("input" to "$EXCHANGE_KEY-dp-mp-double-blinded-joinkeys")
          )
        }
    }
  }
}
