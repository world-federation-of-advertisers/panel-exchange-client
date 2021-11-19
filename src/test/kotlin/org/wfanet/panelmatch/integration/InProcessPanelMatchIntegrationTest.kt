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
import java.nio.file.Path
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.measurement.api.v2alpha.RecurringExchangeKey
import org.wfanet.measurement.api.v2alpha.ResourceKey
import org.wfanet.measurement.api.v2alpha.copy
import org.wfanet.measurement.common.parseTextProto
import org.wfanet.measurement.common.testing.chainRulesSequentially
import org.wfanet.measurement.common.toProtoDate
import org.wfanet.measurement.integration.deploy.gcloud.buildKingdomSpannerEmulatorDatabaseRule
import org.wfanet.measurement.integration.deploy.gcloud.buildSpannerInProcessKingdom

private const val SCHEDULE = "@daily"
private const val API_VERSION = "v2alpha"
private val TODAY: Date = LocalDate.now().toProtoDate()
private val DAEMONS_DELAY_DURATION = Duration.ofSeconds(1).toMillis()

/** E2E Test for Panel Match that everything is wired up and working properly. */
@RunWith(JUnit4::class)
class InProcessPanelMatchIntegrationTest {
  private val databaseRule = buildKingdomSpannerEmulatorDatabaseRule()
  private val inProcessKingdom = buildSpannerInProcessKingdom(databaseRule, Clock.systemUTC())
  private val resourceSetup = inProcessKingdom.panelMatchResourceSetup

  @get:Rule
  val ruleChain: TestRule by lazy { chainRulesSequentially(databaseRule, inProcessKingdom) }

  @get:Rule val dataProviderFolder = TemporaryFolder()
  @get:Rule val modelProviderFolder = TemporaryFolder()
  @get:Rule val sharedFolder = TemporaryFolder()

  private data class ProviderContext(
    val key: ResourceKey,
    val privateStoragePath: Path,
    val scope: CoroutineScope
  )

  private lateinit var dataProviderContext: ProviderContext
  private lateinit var modelProviderContext: ProviderContext
  private lateinit var recurringExchangeKey: RecurringExchangeKey

  private fun createScope(name: String): CoroutineScope {
    return CoroutineScope(CoroutineName(name + Dispatchers.Default))
  }

  @Before
  fun setup() = runBlocking {
    val providers =
      resourceSetup.createResourcesForWorkflow(
        exchangeSchedule = SCHEDULE,
        apiVersion = API_VERSION,
        exchangeWorkflow = exchangeWorkflow,
        exchangeDate = TODAY
      )
    recurringExchangeKey = providers.recurringExchangeKey

    dataProviderContext =
      ProviderContext(
        providers.dataProviderKey,
        dataProviderFolder.root.toPath(),
        createScope("EDP_SCOPE")
      )

    modelProviderContext =
      ProviderContext(
        providers.dataProviderKey,
        modelProviderFolder.root.toPath(),
        createScope("MP_SCOPE")
      )
  }

  private fun makeDaemon(
    owner: ProviderContext,
    partner: ProviderContext
  ): ExchangeWorkflowDaemonForTest {
    return ExchangeWorkflowDaemonForTest(
      v2alphaChannel = inProcessKingdom.publicApiChannel,
      provider = owner.key,
      partnerProvider = partner.key,
      recurringExchangeKey = recurringExchangeKey,
      serializedExchangeWorkflow = exchangeWorkflow.toByteString(),
      privateTemporaryDirectory = owner.privateStoragePath,
      sharedTemporaryDirectory = sharedFolder.root.toPath(),
      scope = owner.scope
    )
  }

  @Test
  fun `entire process`() = runBlocking {
    val dataProviderDaemon = makeDaemon(dataProviderContext, modelProviderContext)
    val modelProviderDaemon = makeDaemon(modelProviderContext, dataProviderContext)

    dataProviderDaemon.run()
    modelProviderDaemon.run()

    delay(DAEMONS_DELAY_DURATION)

    dataProviderContext.scope.cancel()
    modelProviderContext.scope.cancel()

    // TODO: assert that the results are correct.
  }

  companion object {
    private val exchangeWorkflow: ExchangeWorkflow

    init {
      val configPath = "config/mini_exchange_workflow.textproto"
      val resource = requireNotNull(this::class.java.getResource(configPath))

      exchangeWorkflow =
        resource
          .openStream()
          .use { input ->
            parseTextProto(input.bufferedReader(), ExchangeWorkflow.getDefaultInstance())
          }
          // TODO(@yunyeng): Think about the tests that start running around midnight.
          .copy { firstExchangeDate = TODAY }
    }
  }
}
