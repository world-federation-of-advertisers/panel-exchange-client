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

package org.wfanet.panelmatch.integration

import com.google.type.Date
import io.grpc.Metadata
import io.grpc.stub.AbstractStub
import io.grpc.stub.MetadataUtils
import java.lang.IllegalArgumentException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.logging.Logger
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.wfanet.measurement.api.v2alpha.DataProviderKey
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttempt
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttemptKey
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttemptsGrpcKt.ExchangeStepAttemptsCoroutineStub
import org.wfanet.measurement.api.v2alpha.ExchangeStepsGrpcKt.ExchangeStepsCoroutineStub
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.measurement.api.v2alpha.ModelProviderKey
import org.wfanet.measurement.common.identity.apiIdToExternalId
import org.wfanet.measurement.common.identity.withPrincipalName
import org.wfanet.measurement.common.parseTextProto
import org.wfanet.measurement.common.testing.chainRulesSequentially
import org.wfanet.measurement.common.toProtoDate
import org.wfanet.measurement.integration.deploy.gcloud.buildKingdomSpannerEmulatorDatabaseRule
import org.wfanet.measurement.integration.deploy.gcloud.buildSpannerInProcessKingdom
import org.wfanet.panelmatch.client.launcher.ApiClient.ClaimedExchangeStep
import org.wfanet.panelmatch.client.launcher.GrpcApiClient
import org.wfanet.panelmatch.client.launcher.Identity
import org.wfanet.panelmatch.client.logger.addToTaskLog

private const val SCHEDULE = "@daily"
private const val API_VERSION = "v2alpha"
private val LAST_WEEK: Date = LocalDate.now().minusDays(7).toProtoDate()
private const val DATA_PROVIDER_ID = "some-data-provider-id"
private val DATA_PROVIDER_IDENTITY =
  Identity(DATA_PROVIDER_ID, ExchangeWorkflow.Party.DATA_PROVIDER)

class InProcessPanelMatchIntegrationTest {
  private val clock: Clock = Clock.fixed(Instant.ofEpochSecond(123456789), ZoneOffset.UTC)

  val databaseRule = buildKingdomSpannerEmulatorDatabaseRule()
  val inProcessKingdom = buildSpannerInProcessKingdom(databaseRule, clock)
  //
  //  private val apiClient by lazy {
  //    GrpcApiClient(
  //      DATA_PROVIDER_IDENTITY,
  //      ExchangeStepsCoroutineStub(inProcessKingdom.publicApiChannel),
  //      ExchangeStepAttemptsCoroutineStub(inProcessKingdom.publicApiChannel),
  //      clock
  //    )
  //  }

  private fun makeClient(identity: Identity, principalName: String): GrpcApiClient {
    return GrpcApiClient(
      identity,
      ExchangeStepsCoroutineStub(inProcessKingdom.publicApiChannel).withPrincipalName(principalName),
      ExchangeStepAttemptsCoroutineStub(inProcessKingdom.publicApiChannel).withPrincipalName(principalName),
      clock
    )
  }

  @get:Rule
  val ruleChain: TestRule by lazy { chainRulesSequentially(databaseRule, inProcessKingdom) }

  val resourceSetup = inProcessKingdom.panelMatchResourceSetup

  private lateinit var dataProviderKey: String
  private lateinit var modelProviderKey: String

  private suspend fun setup() {

    //    val principal = Principal.DataProvider(DataProviderKey(externalIdToApiId(12345L)))

    val providers =
      resourceSetup.createResourcesForWorkflow(
        exchangeSchedule = SCHEDULE,
        apiVersion = API_VERSION,
        exchangeWorkflow = exchangeWorkflow,
        exchangeDate = LAST_WEEK
      )

    dataProviderKey = providers.dataProviderKey
    modelProviderKey = providers.modelProviderKey
  }

  @Test
  fun `entire process`() = runBlocking {
    setup()
//    logger.addToTaskLog(
//      "Setup complete by creating a MP ${modelProviderKey} and an EDP ${dataProviderKey}."
//    )

    val dataProviderIdentity =
      Identity(DataProviderKey.fromName(dataProviderKey)!!.dataProviderId, ExchangeWorkflow.Party.DATA_PROVIDER)
    val modelProviderIdentity =
      Identity(ModelProviderKey.fromName(modelProviderKey)!!.modelProviderId, ExchangeWorkflow.Party.MODEL_PROVIDER)
    val apiClient = makeClient(modelProviderIdentity, modelProviderKey)
    val job = apiClient.claimJob()
    println(job)

//    var numberOfJobs = 0
//    while (true) {
//      val job = apiClient.claimJob()
////      logger.addToTaskLog("Claimed a job with Step Index: ${job.exchangeStep.stepIndex}.")
//      numberOfJobs++
//
//      apiClient.finishJob(job.exchangeStepAttempt, ExchangeStepAttempt.State.SUCCEEDED)
////      logger.addToTaskLog(
////        "Finished the job with Step Index: ${job.exchangeStep.stepIndex} successfully."
////      )
//    }

    //    assertThat(numberOfJobs).isEqualTo(7)
  }

  private suspend fun GrpcApiClient.claimJob(): ClaimedExchangeStep {
    return claimExchangeStep() ?: throw IllegalArgumentException("Couldn't find any job.")
  }

  private suspend fun GrpcApiClient.finishJob(
    attemptKey: ExchangeStepAttemptKey,
    state: ExchangeStepAttempt.State
  ) {
    finishExchangeStepAttempt(
      key = attemptKey,
      finalState = state,
      logEntryMessages = listOfNotNull("")
    )
  }

  companion object {
    private val logger: Logger = Logger.getLogger(this::class.java.name)
    private val exchangeWorkflow: ExchangeWorkflow

    init {
      val configPath = "config/example_exchange_workflow.textproto"
      val resource = this::class.java.getResource(configPath)

      exchangeWorkflow =
        resource.openStream().use { input ->
          parseTextProto(input.bufferedReader(), ExchangeWorkflow.getDefaultInstance())
        }
    }
  }
}
