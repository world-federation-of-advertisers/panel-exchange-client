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

import com.google.protobuf.ByteString
import com.google.type.Date
import java.lang.IllegalArgumentException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.logging.Logger
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.wfanet.measurement.api.v2alpha.DataProviderKey
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttempt
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttemptKey
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.measurement.api.v2alpha.ModelProviderKey
import org.wfanet.measurement.common.parseTextProto
import org.wfanet.measurement.common.testing.chainRulesSequentially
import org.wfanet.measurement.common.toProtoDate
import org.wfanet.measurement.integration.deploy.gcloud.buildKingdomSpannerEmulatorDatabaseRule
import org.wfanet.measurement.integration.deploy.gcloud.buildSpannerInProcessKingdom
import org.wfanet.panelmatch.client.launcher.ApiClient.ClaimedExchangeStep
import org.wfanet.panelmatch.client.launcher.GrpcApiClient
import org.wfanet.panelmatch.common.secrets.testing.TestSecretMap

private const val SCHEDULE = "@daily"
private const val API_VERSION = "v2alpha"
private val TODAY: Date = LocalDate.now().toProtoDate()

class InProcessPanelMatchIntegrationTest {
  private val clock: Clock = Clock.fixed(Instant.ofEpochSecond(123456789), ZoneOffset.UTC)

  val databaseRule = buildKingdomSpannerEmulatorDatabaseRule()
  val inProcessKingdom = buildSpannerInProcessKingdom(databaseRule, clock)

  @get:Rule
  val ruleChain: TestRule by lazy { chainRulesSequentially(databaseRule, inProcessKingdom) }

  private val resourceSetup = inProcessKingdom.panelMatchResourceSetup

  private lateinit var dataProviderKey: String
  private lateinit var modelProviderKey: String
  private lateinit var recurringExchangeApiId: String

  @Before
  fun setup() = runBlocking {
    val providers =
      resourceSetup.createResourcesForWorkflow(
        exchangeSchedule = SCHEDULE,
        apiVersion = API_VERSION,
        exchangeWorkflow = exchangeWorkflow,
        exchangeDate = TODAY
      )

    dataProviderKey = providers.dataProviderKey
    modelProviderKey = providers.modelProviderKey
    recurringExchangeApiId = providers.recurringExchangeApiId
  }

  @Test
  fun `entire process`() = runBlocking {
    //    logger.addToTaskLog(
    //      "Setup complete by creating a MP ${modelProviderKey} and an EDP ${dataProviderKey}."
    //    )

    val map = mutableMapOf<String, ByteString>(
      Pair(recurringExchangeApiId, exchangeWorkflow.toByteString())
    )

    val edpDaemon =
      ExchangeWorkflowDaemonFromTest(
        channel = inProcessKingdom.publicApiChannel,
        providerKey = DataProviderKey.fromName(dataProviderKey)!!,
        taskTimeoutDuration = Duration.ofSeconds(3),
        pollingInterval = Duration.ofSeconds(15),
        validExchangeWorkflow = TestSecretMap(map),
        storageDirectory = "/"
      )
    val mpDaemon =
      ExchangeWorkflowDaemonFromTest(
        channel = inProcessKingdom.publicApiChannel,
        providerKey = ModelProviderKey.fromName(modelProviderKey)!!,
        taskTimeoutDuration = Duration.ofSeconds(3),
        pollingInterval = Duration.ofSeconds(15),
        validExchangeWorkflow = TestSecretMap(map),
        storageDirectory = "/"
      )

    val attemptKey = java.util.UUID.randomUUID().toString()
    //    val one = async(CoroutineName(attemptKey) + Dispatchers.Default) {
    //      println("FIRST ONE????")
    //    }
    val attemptKey1 = java.util.UUID.randomUUID().toString()
    val job1 = launch { edpDaemon.run() }
    //    val attemptKey2 = java.util.UUID.randomUUID().toString()
    //    val three = async(CoroutineName(attemptKey2) + Dispatchers.Default) {
    //      println("SECONDDDDD ONE?!!!!!???")
    //    }
    val attemptKey3 = java.util.UUID.randomUUID().toString()
    val job2 = launch { mpDaemon.run() }
    //    println("The answer is ${one.await()}. ${two.await()}, ${three.await()}, ${four.await()}")

    val job3 = launch { println("FIRST ONE????") }

    job1.join()
    job2.join()
    job3.join()

    //    val job = apiClient.claimJob()
    //    println(job)

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
