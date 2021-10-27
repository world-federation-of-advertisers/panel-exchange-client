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

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import com.google.type.Date
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
import org.wfanet.measurement.api.v2alpha.DataProviderKey
import org.wfanet.measurement.api.v2alpha.ExchangeKey
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.measurement.api.v2alpha.ModelProviderKey
import org.wfanet.measurement.api.v2alpha.RecurringExchangeKey
import org.wfanet.measurement.api.v2alpha.copy
import org.wfanet.measurement.common.parseTextProto
import org.wfanet.measurement.common.testing.chainRulesSequentially
import org.wfanet.measurement.common.toLocalDate
import org.wfanet.measurement.common.toProtoDate
import org.wfanet.measurement.integration.deploy.gcloud.buildKingdomSpannerEmulatorDatabaseRule
import org.wfanet.measurement.integration.deploy.gcloud.buildSpannerInProcessKingdom
import org.wfanet.panelmatch.client.storage.FileSystemStorageFactory
import org.wfanet.panelmatch.client.storage.StorageDetailsKt.fileStorage
import org.wfanet.panelmatch.client.storage.storageDetails
import org.wfanet.panelmatch.common.secrets.testing.TestSecretMap
import org.wfanet.panelmatch.common.storage.createBlob
import org.wfanet.panelmatch.common.toByteString

private const val SCHEDULE = "@daily"
private const val API_VERSION = "v2alpha"
private val CLOCK: Clock = Clock.systemUTC()
private val TODAY: Date = LocalDate.now().toProtoDate()
private val HKDF_PEPPER = "hkdf-pepper".toByteString()
private val TASK_TIMEOUT_DURATION = Duration.ofSeconds(3)
private val POLLING_INTERVAL = Duration.ofSeconds(3)
private val DAEMONS_DELAY_DURATION = Duration.ofSeconds(1).toMillis()

/** E2E Test for Panel Match that everything is wired up and working properly. */
@RunWith(JUnit4::class)
class InProcessPanelMatchIntegrationTest {
  private val databaseRule = buildKingdomSpannerEmulatorDatabaseRule()
  private val inProcessKingdom = buildSpannerInProcessKingdom(databaseRule, CLOCK)
  private val resourceSetup = inProcessKingdom.panelMatchResourceSetup

  @get:Rule val temporaryFolder = TemporaryFolder()
  @get:Rule
  val ruleChain: TestRule by lazy { chainRulesSequentially(databaseRule, inProcessKingdom) }

  private lateinit var dataProviderKey: DataProviderKey
  private lateinit var modelProviderKey: ModelProviderKey
  private lateinit var recurringExchangeKey: RecurringExchangeKey

  private fun createFolder(provider: String, exchangeKey: ExchangeKey) {
    temporaryFolder.newFolder(
      provider,
      "recurringExchanges",
      exchangeKey.recurringExchangeId,
      "exchanges",
      exchangeKey.exchangeId
    )
  }

  private fun createScope(name: String): CoroutineScope {
    return CoroutineScope(CoroutineName(name + Dispatchers.Default))
  }

  private fun Date.format(): String {
    return toLocalDate().toString()
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
    dataProviderKey = providers.dataProviderKey
    modelProviderKey = providers.modelProviderKey
    recurringExchangeKey = providers.recurringExchangeKey
  }

  @Test
  fun `entire process`() = runBlocking {
    val recurringExchangeId = recurringExchangeKey.recurringExchangeId
    val secretMap =
      TestSecretMap(
        mutableMapOf<String, ByteString>(Pair(recurringExchangeId, exchangeWorkflow.toByteString()))
      )

    val exchangeKey = ExchangeKey(recurringExchangeId, TODAY.format())

    temporaryFolder.create()
    val edpFolder = temporaryFolder.newFolder("edp")
    val mpFolder = temporaryFolder.newFolder("mp")
    createFolder("edp", exchangeKey)
    createFolder("mp", exchangeKey)

    val edpStorageDetails = storageDetails { file = fileStorage { path = edpFolder.path } }
    val mpStorageDetails = storageDetails { file = fileStorage { path = mpFolder.path } }
    val edpStorageFactory =
      FileSystemStorageFactory(storageDetails = edpStorageDetails, exchangeKey = exchangeKey)
    val mpStorageFactory =
      FileSystemStorageFactory(storageDetails = mpStorageDetails, exchangeKey = exchangeKey)

    // TODO(@yunyeng): Build storage from InputBlobs map.
    val edpStorage = edpStorageFactory.build()
    edpStorage.createBlob("edp-hkdf-pepper", HKDF_PEPPER)

    val edpScope = createScope("EDP SCOPE")
    val edpDaemon =
      ExchangeWorkflowDaemonForTest(
        clock = CLOCK,
        scope = edpScope,
        privateStorageFactory = edpStorageFactory,
        validExchangeWorkflows = secretMap,
        channel = inProcessKingdom.publicApiChannel,
        providerKey = dataProviderKey,
        taskTimeoutDuration = TASK_TIMEOUT_DURATION,
        pollingInterval = POLLING_INTERVAL,
      )

    val mpScope = createScope("MP SCOPE")
    val mpDaemon =
      ExchangeWorkflowDaemonForTest(
        clock = CLOCK,
        scope = mpScope,
        privateStorageFactory = mpStorageFactory,
        validExchangeWorkflows = secretMap,
        channel = inProcessKingdom.publicApiChannel,
        providerKey = modelProviderKey,
        taskTimeoutDuration = TASK_TIMEOUT_DURATION,
        pollingInterval = POLLING_INTERVAL,
      )
    edpDaemon.run()
    mpDaemon.run()
    delay(DAEMONS_DELAY_DURATION)
    edpScope.cancel()
    mpScope.cancel()

    assertThat(edpDaemon.apiClient.claimExchangeStep()).isNull()
  }

  companion object {
    private val exchangeWorkflow: ExchangeWorkflow

    init {
      val configPath = "config/mini_exchange_workflow.textproto"
      val resource = this::class.java.getResource(configPath)

      exchangeWorkflow =
        resource
          .openStream()
          .use { input ->
            parseTextProto(input.bufferedReader(), ExchangeWorkflow.getDefaultInstance())
          }
          // TODO(@yunyeng): Think about the tests that start running around midnight.
          .copy { this.firstExchangeDate = TODAY }
    }
  }
}
