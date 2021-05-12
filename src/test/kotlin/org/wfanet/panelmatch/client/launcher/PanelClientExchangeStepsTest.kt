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

package org.wfanet.panelmatch.client.launcher

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
import org.wfanet.measurement.api.v2alpha.ExchangeStep
import org.wfanet.measurement.api.v2alpha.ExchangeStepsGrpcKt.ExchangeStepsCoroutineImplBase as ExchangeStepsCoroutineService
import org.wfanet.measurement.api.v2alpha.ExchangeStepsGrpcKt.ExchangeStepsCoroutineStub
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow.Step as ExchangeWorkflowSteps
import org.wfanet.measurement.api.v2alpha.FindReadyExchangeStepRequest
import org.wfanet.measurement.api.v2alpha.FindReadyExchangeStepResponse
import org.wfanet.measurement.common.grpc.testing.GrpcTestServerRule
import org.wfanet.panelmatch.client.exchangetasks.DataProvider
import org.wfanet.panelmatch.client.exchangetasks.PanelProvider

/** Test the double-blinded key exchange between Model Provider and Data Provider. */
private const val DATA_PROVIDER_ID = "1"
private const val MODEL_PROVIDER_ID = "2"
private const val EXCHANGE_ID = "1"

@RunWith(JUnit4::class)
class PanelClientExchangeStepsTest {
    private val EDP_0_SECRET_KEY = ByteString.copyFromUtf8("random-edp-string-0")
    private val PP_0_SECRET_KEY = ByteString.copyFromUtf8("random-pp-string-0")
    private val joinkeys =
        listOf<ByteString>(
            ByteString.copyFromUtf8("some joinkey0"),
            ByteString.copyFromUtf8("some joinkey1"),
            ByteString.copyFromUtf8("some joinkey2"),
            ByteString.copyFromUtf8("some joinkey3"),
            ByteString.copyFromUtf8("some joinkey4")
        )
    private val WORKFLOW_ENCRYPT_STEP =
      ExchangeWorkflowSteps.newBuilder()
        .build()
    private val WORKFLOW_ENCRYPT =
        ExchangeWorkflow.newBuilder()
            .addSteps(
              ExchangeWorkflow.Step.newBuilder()
            )
/*            .apply {
              step = WORKFLOW_ENCRYPT_STEP
            }*/
            .build()
    private val EXCHANGE_STEP_ENCRYPT =
        ExchangeStep.newBuilder()
            .apply {
                keyBuilder.apply { step = ExchangeWorkflow.Step.newBuilder().build() }
                state = ExchangeStep.State.READY
            }
            .build()
    private val ENCRYPT_AND_SHARE_STEP_RESSPONSE =
        FindReadyExchangeStepResponse.newBuilder()
            .apply { exchangeStep = EXCHANGE_STEP_ENCRYPT }
            .build()
    // .setExchangeStep(EXCHANGE_STEP).build()
    private val REQUEST_WITH_DATA_PROVIDER =
        FindReadyExchangeStepRequest.newBuilder()
            .apply { dataProviderBuilder.dataProviderId = DATA_PROVIDER_ID }
            .build()
    private val REQUEST_WITH_MODEL_PROVIDER =
        FindReadyExchangeStepRequest.newBuilder()
            .apply { modelProviderBuilder.modelProviderId = MODEL_PROVIDER_ID }
            .build()

    private val EMPTY_RESPONSE = FindReadyExchangeStepResponse.newBuilder().build()
    private val exchangeStepsServiceMock: ExchangeStepsCoroutineService =
        mock(useConstructor = UseConstructor.parameterless())
    @get:Rule val grpcTestServerRule = GrpcTestServerRule { addService(exchangeStepsServiceMock) }

    private val exchangeStepsStub: ExchangeStepsCoroutineStub by lazy {
        ExchangeStepsCoroutineStub(grpcTestServerRule.channel)
    }

    @Test
    fun `Double Blind Key Exchange For One DP and one PP`() = runBlocking {
        val dataProvider = DataProvider()
        val panelProvider = PanelProvider()
        val dataProviderLauncher =
            ExchangeStepLauncher(
                exchangeStepsClient = exchangeStepsStub,
                id = DATA_PROVIDER_ID,
                partyType = PartyType.DATA_PROVIDER,
                clock = Clock.systemUTC()
            )
        val panelProviderLauncher =
            ExchangeStepLauncher(
                exchangeStepsClient = exchangeStepsStub,
                id = MODEL_PROVIDER_ID,
                partyType = PartyType.DATA_PROVIDER,
                clock = Clock.systemUTC()
            )
        whenever(exchangeStepsServiceMock.findReadyExchangeStep(any())).thenReturn(ENCRYPT_AND_SHARE_STEP_RESSPONSE)
        val exchangeStep = panelProviderLauncher.findAndRunExchangeStep()
        /*//assertThat(exchangeStep).isEqualTo(EXCHANGE_STEP)
        //verify(exchangeStepsServiceMock, times(1)).findReadyExchangeStep(REQUEST_WITH_DATA_PROVIDER)

        val (dataProviders, campaigns) = kingdom.populateDatabases()

        logger.info("Starting data provider")
        dataProviderRule.startDataProvider(
          dataProviders[0],
        )

        logger.info("Starting panel provider")
        panelProviderRule.startPanelProvider(
          panelProviders[1],
        )

        // Now wait until the computation is done.
        val exchangeResults: ExchangeResults =
          pollFor(timeoutMillis = 5_000) {
            kingdomDatabases
              .reportDatabase
              .streamReports(
                filter = streamReportsFilter(states = listOf(Report.ReportState.SUCCEEDED)),
                limit = 1
              )
              .singleOrNull()
          }

        assertThat(exchangeResults)
          .comparingExpectedFieldsOnly()
          .ignoringRepeatedFieldOrder()
          .isEqualTo(
            Report.newBuilder()
              .apply {
                reportDetailsBuilder.apply {
                  addAllConfirmedDuchies(DUCHY_IDS)
                  reportDetailsBuilder.resultBuilder.apply {
                    reach = 13L
                    putFrequency(6L, 0.3)
                    putFrequency(3L, 0.7)
                  }
                }
              }
              .build()
          )*/
    }
}
