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

import com.google.common.truth.extensions.proto.ProtoTruth.assertThat
import com.nhaarman.mockitokotlin2.UseConstructor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.measurement.api.v2alpha.ExchangeStep
import org.wfanet.measurement.api.v2alpha.ExchangeStepsGrpcKt.ExchangeStepsCoroutineStub
import org.wfanet.measurement.api.v2alpha.FindReadyExchangeStepResponse
import org.wfanet.measurement.common.grpc.testing.GrpcTestServerRule
import org.wfanet.measurement.api.v2alpha.ExchangeStepsGrpcKt.ExchangeStepsCoroutineImplBase as ExchangeStepsCoroutineService

private const val DATA_PROVIDER_ID = "1"
private const val EXCHANGE_ID = "1"

val EXCHANGE_STEP = ExchangeStep.newBuilder()
  .setState(ExchangeStep.State.READY)
  .setKey(ExchangeStep.Key.newBuilder().setExchangeId(EXCHANGE_ID))
  .build()
private val RESPONSE =
  FindReadyExchangeStepResponse.newBuilder().setExchangeStep(EXCHANGE_STEP).build()
private val EMPTY_RESPONSE = FindReadyExchangeStepResponse.newBuilder().build()

@RunWith(JUnit4::class)
class ExchangeStepLauncherTest {

  private val exchangeStepsServiceMock: ExchangeStepsCoroutineService =
    mock(useConstructor = UseConstructor.parameterless())
  @get:Rule val grpcTestServerRule = GrpcTestServerRule { addService(exchangeStepsServiceMock) }

  private val exchangeStepsStub: ExchangeStepsCoroutineStub by lazy {
    ExchangeStepsCoroutineStub(grpcTestServerRule.channel)
  }

  @Test
  fun findExchangeTask() = runBlocking {
    val launcher = ExchangeStepLauncher(exchangeStepsStub)
    whenever(exchangeStepsServiceMock.findReadyExchangeStep(any()))
      .thenReturn(RESPONSE)
    val task = launcher.findExchangeTask(DATA_PROVIDER_ID)
    assertThat(task).isEqualTo(EXCHANGE_STEP)
  }

  @Test
  fun `findExchangeTask without exchangeStep`() {
    val launcher = ExchangeStepLauncher(exchangeStepsStub)
    Assert.assertThrows(NoSuchElementException::class.java) {
      runBlocking {
        whenever(exchangeStepsServiceMock.findReadyExchangeStep(any()))
          .thenReturn(EMPTY_RESPONSE)
        launcher.findExchangeTask(DATA_PROVIDER_ID)
      }
    }
  }
}
