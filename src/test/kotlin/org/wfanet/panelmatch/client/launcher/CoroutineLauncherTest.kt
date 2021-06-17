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
import kotlinx.coroutines.delay
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
import org.wfanet.panelmatch.client.storage.Storage.STORAGE_CLASS
import org.wfanet.panelmatch.client.storage.batchRead
import org.wfanet.panelmatch.client.storage.batchWrite
import org.wfanet.panelmatch.protocol.common.makeSerializedSharedInputs

@RunWith(JUnit4::class)
class CoroutineLauncherTest {

  @Test
  fun `test input task`() {
    System.setProperty("PRIVATE_STORAGE_TYPE", "FILESYSTEM")
    System.setProperty("PRIVATE_FILESYSTEM_PATH", "${System.getenv("TEST_TMPDIR")}/private")
    val apiClient: ApiClient = mock()
    val EXCHANGE_KEY = java.util.UUID.randomUUID().toString()
    val ATTEMPT_KEY = java.util.UUID.randomUUID().toString()
    runBlocking {
      whenever(apiClient.finishExchangeStepAttempt(any(), any(), any())).thenReturn(Unit)
      val outputLabels = mapOf("output" to "mp-crypto-key")
      val testStep =
        TestStep(
          apiClient = apiClient,
          exchangeKey = EXCHANGE_KEY,
          exchangeStepAttemptKey = ATTEMPT_KEY,
          privateInputLabels = mapOf("input" to "encryption-key"),
          privateOutputLabels = outputLabels,
          stepType = ExchangeWorkflow.Step.StepCase.INPUT_STEP
        )
      testStep.buildAndExecuteJob()
      val argumentException =
        assertFailsWith(IllegalArgumentException::class) {
          batchRead(
            storageClass = STORAGE_CLASS.PRIVATE,
            exchangeKey = EXCHANGE_KEY,
            step = testStep.build(),
            inputLabels = mapOf("input" to "mp-crypto-key")
          )
        }
      verify(apiClient, times(0)).finishExchangeStepAttempt(any(), any(), any())
      // Model Provider asynchronously writes data
      batchWrite(
        storageClass = STORAGE_CLASS.PRIVATE,
        exchangeKey = EXCHANGE_KEY,
        step = testStep.build(),
        outputLabels = outputLabels,
        data = mapOf("output" to MP_0_SECRET_KEY)
      )
      delay(600)
      val readValues =
        requireNotNull(
          batchRead(
            storageClass = STORAGE_CLASS.PRIVATE,
            exchangeKey = EXCHANGE_KEY,
            step = testStep.build(),
            inputLabels = mapOf("input" to "mp-crypto-key")
          )["input"]
        )
      assertThat(readValues).isEqualTo(MP_0_SECRET_KEY)
      verify(apiClient, times(0))
        .finishExchangeStepAttempt(any(), eq(ExchangeStepAttempt.State.FAILED), any())
      verify(apiClient, times(1))
        .finishExchangeStepAttempt(any(), eq(ExchangeStepAttempt.State.SUCCEEDED), any())
    }
  }

  @Test
  fun `test crypto task with private inputs and private output`() {
    System.setProperty("PRIVATE_STORAGE_TYPE", "FILESYSTEM")
    System.setProperty("PRIVATE_FILESYSTEM_PATH", "${System.getenv("TEST_TMPDIR")}/private")
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
            mapOf("encryption-key" to "mp-crypto-key", "unencrypted-data" to "mp-joinkeys"),
          privateOutputLabels = mapOf("encrypted-data" to "mp-single-blinded-joinkeys"),
          stepType = ExchangeWorkflow.Step.StepCase.ENCRYPT_STEP
        )
      batchWrite(
        storageClass = STORAGE_CLASS.PRIVATE,
        exchangeKey = EXCHANGE_KEY,
        step = testStep.build(),
        outputLabels = mapOf("output" to "mp-crypto-key"),
        data = mapOf("output" to MP_0_SECRET_KEY)
      )
      batchWrite(
        storageClass = STORAGE_CLASS.PRIVATE,
        exchangeKey = EXCHANGE_KEY,
        step = testStep.build(),
        outputLabels = mapOf("output" to "mp-joinkeys"),
        data = mapOf("output" to makeSerializedSharedInputs(JOIN_KEYS))
      )
      testStep.buildAndExecuteJob()
      val argumentException =
        assertFailsWith(IllegalArgumentException::class) {
          batchRead(
            storageClass = STORAGE_CLASS.PRIVATE,
            exchangeKey = EXCHANGE_KEY,
            step = testStep.build(),
            inputLabels = mapOf("input" to "mp-single-blinded-joinkeys")
          )
        }
      verify(apiClient, times(0)).finishExchangeStepAttempt(any(), any(), any())
      delay(600)
      val readValues =
        requireNotNull(
          batchRead(
            storageClass = STORAGE_CLASS.PRIVATE,
            exchangeKey = EXCHANGE_KEY,
            step = testStep.build(),
            inputLabels = mapOf("input" to "mp-single-blinded-joinkeys")
          )["input"]
        )
      verify(apiClient, times(0))
        .finishExchangeStepAttempt(any(), eq(ExchangeStepAttempt.State.FAILED), any())
      verify(apiClient, times(1))
        .finishExchangeStepAttempt(any(), eq(ExchangeStepAttempt.State.SUCCEEDED), any())
    }
  }

  @Test
  fun `test crypto task with private and shared inputs and shared and private output`() {
    System.setProperty("PRIVATE_STORAGE_TYPE", "FILESYSTEM")
    System.setProperty("PRIVATE_FILESYSTEM_PATH", "${System.getenv("TEST_TMPDIR")}/private")
    System.setProperty("SHARED_STORAGE_TYPE", "FILESYSTEM")
    System.setProperty("SHARED_FILESYSTEM_PATH", "${System.getenv("TEST_TMPDIR")}/shared")
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
          privateInputLabels = mapOf("encryption-key" to "dp-crypto-key"),
          privateOutputLabels = mapOf("reencrypted-data" to "dp-mp-double-blinded-joinkeys"),
          sharedInputLabels = mapOf("encrypted-data" to "mp-single-blinded-joinkeys"),
          sharedOutputLabels = mapOf("reencrypted-data" to "dp-mp-double-blinded-joinkeys"),
          stepType = ExchangeWorkflow.Step.StepCase.REENCRYPT_STEP
        )
      batchWrite(
        storageClass = STORAGE_CLASS.PRIVATE,
        exchangeKey = EXCHANGE_KEY,
        step = testStep.build(),
        outputLabels = mapOf("output" to "dp-crypto-key"),
        data = mapOf("output" to DP_0_SECRET_KEY)
      )
      batchWrite(
        storageClass = STORAGE_CLASS.SHARED,
        exchangeKey = EXCHANGE_KEY,
        step = testStep.build(),
        outputLabels = mapOf("output" to "mp-single-blinded-joinkeys"),
        data = mapOf("output" to makeSerializedSharedInputs(SINGLE_BLINDED_KEYS))
      )
      testStep.buildAndExecuteJob()
      val argumentException =
        assertFailsWith(IllegalArgumentException::class) {
          batchRead(
            storageClass = STORAGE_CLASS.PRIVATE,
            exchangeKey = EXCHANGE_KEY,
            step = testStep.build(),
            inputLabels = mapOf("input" to "dp-mp-double-blinded-joinkeys")
          )
        }
      verify(apiClient, times(0)).finishExchangeStepAttempt(any(), any(), any())
      delay(600)
      val privateReadValues =
        requireNotNull(
          batchRead(
            storageClass = STORAGE_CLASS.PRIVATE,
            exchangeKey = EXCHANGE_KEY,
            step = testStep.build(),
            inputLabels = mapOf("input" to "dp-mp-double-blinded-joinkeys")
          )["input"]
        )
      val sharedReadValues =
        requireNotNull(
          batchRead(
            storageClass = STORAGE_CLASS.SHARED,
            exchangeKey = EXCHANGE_KEY,
            step = testStep.build(),
            inputLabels = mapOf("input" to "dp-mp-double-blinded-joinkeys")
          )["input"]
        )
      verify(apiClient, times(0))
        .finishExchangeStepAttempt(any(), eq(ExchangeStepAttempt.State.FAILED), any())
      verify(apiClient, times(1))
        .finishExchangeStepAttempt(any(), eq(ExchangeStepAttempt.State.SUCCEEDED), any())
    }
  }

  @Test
  fun `test crypto task with missing shared inputs`() {
    System.setProperty("PRIVATE_STORAGE_TYPE", "FILESYSTEM")
    System.setProperty("PRIVATE_FILESYSTEM_PATH", "${System.getenv("TEST_TMPDIR")}/private")
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
          privateInputLabels = mapOf("encryption-key" to "dp-crypto-key"),
          privateOutputLabels = mapOf("reencrypted-data" to "dp-mp-double-blinded-joinkeys"),
          sharedInputLabels = mapOf("encrypted-data" to "mp-single-blinded-joinkeys"),
          sharedOutputLabels = mapOf("reencrypted-data" to "dp-mp-double-blinded-joinkeys"),
          stepType = ExchangeWorkflow.Step.StepCase.REENCRYPT_STEP
        )
      batchWrite(
        storageClass = STORAGE_CLASS.PRIVATE,
        exchangeKey = EXCHANGE_KEY,
        step = testStep.build(),
        outputLabels = mapOf("output" to "dp-crypto-key"),
        data = mapOf("output" to DP_0_SECRET_KEY)
      )
      testStep.buildAndExecuteJob()
      delay(600)
      verify(apiClient, times(1))
        .finishExchangeStepAttempt(any(), eq(ExchangeStepAttempt.State.FAILED), any())
      verify(apiClient, times(0))
        .finishExchangeStepAttempt(any(), eq(ExchangeStepAttempt.State.SUCCEEDED), any())
      val argumentException1 =
        assertFailsWith(IllegalArgumentException::class) {
          batchRead(
            storageClass = STORAGE_CLASS.PRIVATE,
            exchangeKey = EXCHANGE_KEY,
            step = testStep.build(),
            inputLabels = mapOf("input" to "dp-mp-double-blinded-joinkeys")
          )
        }
    }
  }
}
