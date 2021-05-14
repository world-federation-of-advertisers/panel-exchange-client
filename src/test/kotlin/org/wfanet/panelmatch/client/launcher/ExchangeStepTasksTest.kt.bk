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

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import com.nhaarman.mockitokotlin2.UseConstructor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.whenever
import java.time.Clock
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.measurement.api.v2alpha.ClaimReadyExchangeStepResponse
import org.wfanet.measurement.api.v2alpha.ExchangeStep
import org.wfanet.measurement.api.v2alpha.ExchangeStepsGrpcKt.ExchangeStepsCoroutineImplBase as ExchangeStepsCoroutineService
import org.wfanet.measurement.api.v2alpha.ExchangeStepsGrpcKt.ExchangeStepsCoroutineStub
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow.Party as PartyType
import org.wfanet.measurement.common.grpc.testing.GrpcTestServerRule
import org.wfanet.panelmatch.client.launcher.ExchangeStepLauncher
import org.wfanet.panelmatch.protocol.common.applyCommutativeDecryptionHelper
import org.wfanet.panelmatch.protocol.common.applyCommutativeEncryptionHelper
import org.wfanet.panelmatch.protocol.common.reApplyCommutativeEncryptionHelper
import wfanet.panelmatch.protocol.protobuf.SharedInputs

/** Test the double-blinded key exchange between Model Provider and Data Provider. */
private const val DATA_PROVIDER_ID = "1"
private const val MODEL_PROVIDER_ID = "2"
private const val EXCHANGE_ID = "1"

@RunWith(JUnit4::class)
class ExchangeTasksTest {
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
  private val exchangeStepsServiceMock: ExchangeStepsCoroutineService =
    mock(useConstructor = UseConstructor.parameterless())
  @get:Rule val grpcTestServerRule = GrpcTestServerRule { addService(exchangeStepsServiceMock) }

  private val exchangeStepsStub: ExchangeStepsCoroutineStub by lazy {
    ExchangeStepsCoroutineStub(grpcTestServerRule.channel)
  }

  @Test
  // TODO Currently this test manually sets up each response. Rewrite as polling service.
  fun `Double Blind Key Exchange For One DP and one PP`() = runBlocking {
    // Init the participating parties
    val dataProviderLauncher0 =
      ExchangeStepLauncher(
        exchangeStepsClient = exchangeStepsStub,
        id = DATA_PROVIDER_ID,
        partyType = PartyType.DATA_PROVIDER,
        clock = Clock.systemUTC()
      )
    val modelProviderLauncher0 =
      ExchangeStepLauncher(
        exchangeStepsClient = exchangeStepsStub,
        id = MODEL_PROVIDER_ID,
        partyType = PartyType.MODEL_PROVIDER,
        clock = Clock.systemUTC()
      )

    // Mock up encrypt response for MP
    val EXCHANGE_STEP_ENCRYPT_INPUT =
      mapOf(
        "crypto-key" to MP_0_SECRET_KEY,
        "data" to SharedInputs.newBuilder().addAllData(joinkeys).build().toByteString()
      )
    val EXCHANGE_STEP_ENCRYPT =
      ExchangeStep.newBuilder()
        .apply {
          step =
            ExchangeWorkflow.Step.newBuilder()
              .apply {
                stepId = "mp-blind"
                party = PartyType.MODEL_PROVIDER
                encryptAndShare = ExchangeWorkflow.Step.EncryptAndShareStep.newBuilder().build()
              }
              .build()
          state = ExchangeStep.State.READY
        }
        .putAllSharedInputs(EXCHANGE_STEP_ENCRYPT_INPUT)
        .build()
    val ENCRYPT_AND_SHARE_STEP_RESPONSE =
      ClaimReadyExchangeStepResponse.newBuilder()
        .apply { exchangeStep = EXCHANGE_STEP_ENCRYPT }
        .build()
    whenever(exchangeStepsServiceMock.claimReadyExchangeStep(any()))
      .thenReturn(ENCRYPT_AND_SHARE_STEP_RESPONSE)
    val exchangeStepResults0 = modelProviderLauncher0.findAndRunExchangeStep()["result"]
    val encryptedJoinKeys = applyCommutativeEncryptionHelper(MP_0_SECRET_KEY, joinkeys)
    assertThat(encryptedJoinKeys)
      .isEqualTo(SharedInputs.parseFrom(exchangeStepResults0).getDataList())

    // Mock up reEncrypt response for DP
    val EXCHANGE_STEP_REENCRYPT_INPUT =
      mapOf("crypto-key" to DP_0_SECRET_KEY, "data" to exchangeStepResults0)
    val EXCHANGE_STEP_REENCRYPT =
      ExchangeStep.newBuilder()
        .apply {
          step =
            ExchangeWorkflow.Step.newBuilder()
              .apply {
                stepId = "dp-double-blind"
                party = PartyType.DATA_PROVIDER
                encryptAndShare = ExchangeWorkflow.Step.EncryptAndShareStep.newBuilder().build()
              }
              .build()
          state = ExchangeStep.State.READY
        }
        .putAllSharedInputs(EXCHANGE_STEP_REENCRYPT_INPUT)
        .build()
    val REENCRYPT_AND_SHARE_STEP_RESPONSE =
      ClaimReadyExchangeStepResponse.newBuilder()
        .apply { exchangeStep = EXCHANGE_STEP_REENCRYPT }
        .build()
    whenever(exchangeStepsServiceMock.claimReadyExchangeStep(any()))
      .thenReturn(REENCRYPT_AND_SHARE_STEP_RESPONSE)
    val exchangeStepResults1 = dataProviderLauncher0.findAndRunExchangeStep()["result"]
    val reEncryptedJoinKeys = reApplyCommutativeEncryptionHelper(DP_0_SECRET_KEY, encryptedJoinKeys)
    assertThat(reEncryptedJoinKeys)
      .isEqualTo(SharedInputs.parseFrom(exchangeStepResults1).getDataList())

    // Mock up decrypt response for DP
    val EXCHANGE_STEP_DECRYPT_INPUT =
      mapOf("crypto-key" to MP_0_SECRET_KEY, "data" to exchangeStepResults1)
    val EXCHANGE_STEP_DECRYPT =
      ExchangeStep.newBuilder()
        .apply {
          step =
            ExchangeWorkflow.Step.newBuilder()
              .apply {
                stepId = "mp-decrypt"
                party = PartyType.MODEL_PROVIDER
                decrypt = ExchangeWorkflow.Step.DecryptStep.newBuilder().build()
              }
              .build()
          state = ExchangeStep.State.READY
        }
        .putAllSharedInputs(EXCHANGE_STEP_DECRYPT_INPUT)
        .build()
    val DECRYPT_STEP_RESPONSE =
      ClaimReadyExchangeStepResponse.newBuilder()
        .apply { exchangeStep = EXCHANGE_STEP_DECRYPT }
        .build()
    whenever(exchangeStepsServiceMock.claimReadyExchangeStep(any()))
      .thenReturn(DECRYPT_STEP_RESPONSE)
    val exchangeStepResults2 = dataProviderLauncher0.findAndRunExchangeStep()["result"]
    val decryptedJoinKeys = applyCommutativeDecryptionHelper(MP_0_SECRET_KEY, reEncryptedJoinKeys)
    assertThat(decryptedJoinKeys)
      .isEqualTo(SharedInputs.parseFrom(exchangeStepResults2).getDataList())
  }
}
