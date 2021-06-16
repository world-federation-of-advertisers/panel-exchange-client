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
import com.nhaarman.mockitokotlin2.mock
import kotlin.test.assertFailsWith
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.panelmatch.client.launcher.testing.DOUBLE_BLINDED_KEYS
import org.wfanet.panelmatch.client.launcher.testing.DP_0_SECRET_KEY
import org.wfanet.panelmatch.client.launcher.testing.JOIN_KEYS
import org.wfanet.panelmatch.client.launcher.testing.LOOKUP_KEYS
import org.wfanet.panelmatch.client.launcher.testing.MP_0_SECRET_KEY
import org.wfanet.panelmatch.client.launcher.testing.SINGLE_BLINDED_KEYS
import org.wfanet.panelmatch.client.launcher.testing.TestStep
import org.wfanet.panelmatch.client.storage.Storage.STORAGE_TYPE
import org.wfanet.panelmatch.client.storage.batchRead
import org.wfanet.panelmatch.client.storage.batchWrite
import org.wfanet.panelmatch.protocol.common.makeSerializedSharedInputs
import org.wfanet.panelmatch.protocol.common.parseSerializedSharedInputs

@RunWith(JUnit4::class)
class ExchangeTaskMapperTest {
  val apiClient: ApiClient = mock()

  @Test
  fun `test encrypt exchange step`() = runBlocking {
    val EXCHANGE_KEY = java.util.UUID.randomUUID().toString()
    val ATTEMPT_KEY = java.util.UUID.randomUUID().toString()
    val testStep =
      TestStep(
        apiClient = apiClient,
        exchangeKey = EXCHANGE_KEY,
        exchangeStepAttemptKey = ATTEMPT_KEY,
        privateInputLabels =
          mapOf("encryption-key" to "mp-crypto-key", "unencrypted-data" to "mp-joinkeys"),
        sharedOutputLabels = mapOf("encrypted-data" to "mp-single-blinded-joinkeys"),
        stepType = ExchangeWorkflow.Step.StepCase.ENCRYPT_STEP
      )
    batchWrite(
      storageType = STORAGE_TYPE.PRIVATE,
      exchangeKey = EXCHANGE_KEY,
      step = testStep.build(),
      outputLabels = mapOf("output" to "mp-crypto-key"),
      data = mapOf("output" to MP_0_SECRET_KEY)
    )
    batchWrite(
      storageType = STORAGE_TYPE.PRIVATE,
      exchangeKey = EXCHANGE_KEY,
      step = testStep.build(),
      outputLabels = mapOf("output" to "mp-joinkeys"),
      data = mapOf("output" to makeSerializedSharedInputs(JOIN_KEYS))
    )
    delay(10)
    testStep.buildAndExecute()
    delay(10)
    val singleBlindedKeys =
      requireNotNull(
        batchRead(
          storageType = STORAGE_TYPE.SHARED,
          exchangeKey = EXCHANGE_KEY,
          step = testStep.build(),
          inputLabels = mapOf("input" to "mp-single-blinded-joinkeys")
        )["input"]
      )
    assertThat(parseSerializedSharedInputs(singleBlindedKeys)).isEqualTo(SINGLE_BLINDED_KEYS)
  }

  @Test
  fun `test reencrypt exchange task`() = runBlocking {
    val EXCHANGE_KEY = java.util.UUID.randomUUID().toString()
    val ATTEMPT_KEY = java.util.UUID.randomUUID().toString()
    val testStep =
      TestStep(
        apiClient = apiClient,
        exchangeKey = EXCHANGE_KEY,
        exchangeStepAttemptKey = ATTEMPT_KEY,
        privateInputLabels = mapOf("encryption-key" to "dp-crypto-key"),
        sharedInputLabels = mapOf("encrypted-data" to "mp-single-blinded-joinkeys"),
        sharedOutputLabels = mapOf("reencrypted-data" to "dp-mp-double-blinded-joinkeys"),
        stepType = ExchangeWorkflow.Step.StepCase.REENCRYPT_STEP
      )
    batchWrite(
      storageType = STORAGE_TYPE.PRIVATE,
      exchangeKey = EXCHANGE_KEY,
      step = testStep.build(),
      outputLabels = mapOf("output" to "dp-crypto-key"),
      data = mapOf("output" to DP_0_SECRET_KEY)
    )
    batchWrite(
      storageType = STORAGE_TYPE.SHARED,
      exchangeKey = EXCHANGE_KEY,
      step = testStep.build(),
      outputLabels = mapOf("output" to "mp-single-blinded-joinkeys"),
      data = mapOf("output" to makeSerializedSharedInputs(SINGLE_BLINDED_KEYS))
    )
    delay(10)
    testStep.buildAndExecute()
    delay(10)
    val doubleBlindedKeys =
      requireNotNull(
        batchRead(
          storageType = STORAGE_TYPE.SHARED,
          exchangeKey = EXCHANGE_KEY,
          step = testStep.build(),
          inputLabels = mapOf("input" to "dp-mp-double-blinded-joinkeys")
        )["input"]
      )
    assertThat(parseSerializedSharedInputs(doubleBlindedKeys)).isEqualTo(DOUBLE_BLINDED_KEYS)
  }

  @Test
  fun `test decrypt exchange step that only mp has access to lookup keys`() = runBlocking {
    val EXCHANGE_KEY = java.util.UUID.randomUUID().toString()
    val ATTEMPT_KEY = java.util.UUID.randomUUID().toString()
    val testStep =
      TestStep(
        apiClient = apiClient,
        exchangeKey = EXCHANGE_KEY,
        exchangeStepAttemptKey = ATTEMPT_KEY,
        privateInputLabels = mapOf("encryption-key" to "mp-crypto-key"),
        sharedInputLabels = mapOf("encrypted-data" to "dp-mp-double-blinded-joinkeys"),
        privateOutputLabels = mapOf("decrypted-data" to "decrypted-data"),
        stepType = ExchangeWorkflow.Step.StepCase.DECRYPT_STEP
      )
    batchWrite(
      storageType = STORAGE_TYPE.PRIVATE,
      exchangeKey = EXCHANGE_KEY,
      step = testStep.build(),
      outputLabels = mapOf("output" to "mp-crypto-key"),
      data = mapOf("output" to MP_0_SECRET_KEY)
    )
    batchWrite(
      storageType = STORAGE_TYPE.SHARED,
      exchangeKey = EXCHANGE_KEY,
      step = testStep.build(),
      outputLabels = mapOf("output" to "dp-mp-double-blinded-joinkeys"),
      data = mapOf("output" to makeSerializedSharedInputs(DOUBLE_BLINDED_KEYS))
    )
    delay(10)
    testStep.buildAndExecute()
    delay(10)
    val argumentException =
      assertFailsWith(IllegalArgumentException::class) {
        batchRead(
          storageType = STORAGE_TYPE.SHARED,
          exchangeKey = EXCHANGE_KEY,
          step = testStep.build(),
          inputLabels = mapOf("input" to "decrypted-data")
        )
      }
    val lookupKeys =
      requireNotNull(
        batchRead(
          storageType = STORAGE_TYPE.PRIVATE,
          exchangeKey = EXCHANGE_KEY,
          step = testStep.build(),
          inputLabels = mapOf("input" to "decrypted-data")
        )["input"]
      )
    assertThat(parseSerializedSharedInputs(lookupKeys)).isEqualTo(LOOKUP_KEYS)
  }
}
