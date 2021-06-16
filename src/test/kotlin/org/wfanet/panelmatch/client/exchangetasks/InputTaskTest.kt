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

package org.wfanet.panelmatch.client.exchangetasks

import com.nhaarman.mockitokotlin2.mock
import java.time.Duration
import kotlin.test.assertFailsWith
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.panelmatch.client.launcher.ApiClient
import org.wfanet.panelmatch.client.launcher.testing.MP_0_SECRET_KEY
import org.wfanet.panelmatch.client.launcher.testing.SINGLE_BLINDED_KEYS
import org.wfanet.panelmatch.client.launcher.testing.TestStep
import org.wfanet.panelmatch.client.storage.Storage.STORAGE_TYPE
import org.wfanet.panelmatch.client.storage.batchWrite
import org.wfanet.panelmatch.protocol.common.makeSerializedSharedInputs

@RunWith(JUnit4::class)
class InputTaskTest {
  val apiClient: ApiClient = mock()
  @Test
  fun `wait on private input`() = runBlocking {
    val EXCHANGE_KEY = java.util.UUID.randomUUID().toString()
    val ATTEMPT_KEY = java.util.UUID.randomUUID().toString()
    val testStep =
      TestStep(
        apiClient = apiClient,
        exchangeKey = EXCHANGE_KEY,
        exchangeStepAttemptKey = ATTEMPT_KEY,
        privateOutputLabels = mapOf("input" to "mp-crypto-key"),
        stepType = ExchangeWorkflow.Step.StepCase.INPUT_STEP,
        timeoutDuration = Duration.ofMillis(500),
        retryDuration = Duration.ofMillis(100)
      )
    val output = coroutineScope {
      async { testStep.buildAndExecute() }
      delay(300)
      batchWrite(
        storageType = STORAGE_TYPE.PRIVATE,
        exchangeKey = EXCHANGE_KEY,
        step = testStep.build(),
        outputLabels = mapOf("output" to "mp-crypto-key"),
        data = mapOf("output" to MP_0_SECRET_KEY)
      )
    }
  }

  @Test
  fun `wait on shared input`() = runBlocking {
    val EXCHANGE_KEY = java.util.UUID.randomUUID().toString()
    val ATTEMPT_KEY = java.util.UUID.randomUUID().toString()
    val testStep =
      TestStep(
        apiClient = apiClient,
        exchangeKey = EXCHANGE_KEY,
        exchangeStepAttemptKey = ATTEMPT_KEY,
        sharedOutputLabels = mapOf("input" to "mp-single-blinded-keys"),
        stepType = ExchangeWorkflow.Step.StepCase.INPUT_STEP,
        timeoutDuration = Duration.ofMillis(500),
        retryDuration = Duration.ofMillis(100)
      )
    val output = coroutineScope {
      async { testStep.buildAndExecute() }
      delay(300)
      batchWrite(
        storageType = STORAGE_TYPE.SHARED,
        exchangeKey = EXCHANGE_KEY,
        step = testStep.build(),
        outputLabels = mapOf("output" to "mp-single-blinded-keys"),
        data = mapOf("output" to makeSerializedSharedInputs(SINGLE_BLINDED_KEYS))
      )
    }
  }

  @Test
  fun `wait on private input fails after timeout`() = runBlocking {
    val EXCHANGE_KEY = java.util.UUID.randomUUID().toString()
    val ATTEMPT_KEY = java.util.UUID.randomUUID().toString()
    val testStep =
      TestStep(
        apiClient = apiClient,
        exchangeKey = EXCHANGE_KEY,
        exchangeStepAttemptKey = ATTEMPT_KEY,
        privateOutputLabels = mapOf("input" to "mp-crypto-key"),
        stepType = ExchangeWorkflow.Step.StepCase.INPUT_STEP,
        timeoutDuration = Duration.ofMillis(500),
        retryDuration = Duration.ofMillis(100)
      )
    val argumentException =
      assertFailsWith(TimeoutCancellationException::class) {
        val output = coroutineScope {
          val job = async { testStep.buildAndExecute() }
          job.await()
        }
      }
  }

  @Test
  fun `wait on shared input fails if party takes too long to write`() = runBlocking {
    val EXCHANGE_KEY = java.util.UUID.randomUUID().toString()
    val ATTEMPT_KEY = java.util.UUID.randomUUID().toString()
    val testStep =
      TestStep(
        apiClient = apiClient,
        exchangeKey = EXCHANGE_KEY,
        exchangeStepAttemptKey = ATTEMPT_KEY,
        sharedOutputLabels = mapOf("input" to "mp-single-blinded-keys"),
        stepType = ExchangeWorkflow.Step.StepCase.INPUT_STEP,
        timeoutDuration = Duration.ofMillis(500),
        retryDuration = Duration.ofMillis(100)
      )
    val argumentException =
      assertFailsWith(TimeoutCancellationException::class) {
        val output = coroutineScope {
          val job = async { testStep.buildAndExecute() }
          delay(600)
          batchWrite(
            storageType = STORAGE_TYPE.SHARED,
            exchangeKey = EXCHANGE_KEY,
            step = testStep.build(),
            outputLabels = mapOf("output" to "mp-single-blinded-keys"),
            data = mapOf("output" to makeSerializedSharedInputs(SINGLE_BLINDED_KEYS))
          )
          job.await()
        }
      }
  }
}
