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
import com.google.common.truth.extensions.proto.ProtoTruth.assertThat
import com.google.protobuf.ByteString
import com.google.protobuf.MessageLite
import com.google.type.Date
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.logging.Logger
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.beam.sdk.values.PCollection
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestRule
import org.wfanet.measurement.api.v2alpha.DataProviderKey
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.measurement.api.v2alpha.ModelProviderKey
import org.wfanet.measurement.api.v2alpha.RecurringExchangeKey
import org.wfanet.measurement.common.parseTextProto
import org.wfanet.measurement.common.testing.chainRulesSequentially
import org.wfanet.measurement.common.toProtoDate
import org.wfanet.measurement.integration.deploy.gcloud.buildKingdomSpannerEmulatorDatabaseRule
import org.wfanet.measurement.integration.deploy.gcloud.buildSpannerInProcessKingdom
import org.wfanet.panelmatch.client.common.testing.eventsOf
import org.wfanet.panelmatch.client.eventpreprocessing.HardCodedDeterministicCommutativeCipherKeyProvider
import org.wfanet.panelmatch.client.eventpreprocessing.HardCodedHkdfPepperProvider
import org.wfanet.panelmatch.client.eventpreprocessing.HardCodedIdentifierHashPepperProvider
import org.wfanet.panelmatch.client.eventpreprocessing.preprocessEventsInPipeline
import org.wfanet.panelmatch.client.privatemembership.DatabaseEntry
import org.wfanet.panelmatch.client.privatemembership.WriteShardedData
import org.wfanet.panelmatch.client.privatemembership.databaseEntries
import org.wfanet.panelmatch.client.privatemembership.databaseEntry
import org.wfanet.panelmatch.client.storage.FileSystemStorageFactory
import org.wfanet.panelmatch.common.ShardedFileName
import org.wfanet.panelmatch.common.beam.testing.BeamTestBase
import org.wfanet.panelmatch.common.compression.UncompressedDictionaryBuilder
import org.wfanet.panelmatch.common.secrets.testing.TestSecretMap
import org.wfanet.panelmatch.common.storage.createBlob
import org.wfanet.panelmatch.common.toByteString

private const val SCHEDULE = "@daily"
private const val API_VERSION = "v2alpha"
private val TODAY: Date = LocalDate.now().toProtoDate()
private const val MAX_BYTE_SIZE = 8
private val IDENTIFIER_HASH_PEPPER_PROVIDER =
  HardCodedIdentifierHashPepperProvider("identifier-hash-pepper".toByteString())
private val HKDF_PEPPER_PROVIDER = HardCodedHkdfPepperProvider("hkdf-pepper".toByteString())
private val CRYPTO_KEY_PROVIDER =
  HardCodedDeterministicCommutativeCipherKeyProvider("crypto-key".toByteString())

class InProcessPanelMatchIntegrationTest : BeamTestBase() {
  private val clock: Clock = Clock.fixed(Instant.ofEpochSecond(123456789), ZoneOffset.UTC)

  @get:Rule val temporaryFolder = TemporaryFolder()

  private val databaseRule = buildKingdomSpannerEmulatorDatabaseRule()
  private val inProcessKingdom = buildSpannerInProcessKingdom(databaseRule, clock)

  @get:Rule
  val ruleChain: TestRule by lazy { chainRulesSequentially(databaseRule, inProcessKingdom) }

  private val resourceSetup = inProcessKingdom.panelMatchResourceSetup

  private lateinit var dataProviderKey: DataProviderKey
  private lateinit var modelProviderKey: ModelProviderKey
  private lateinit var recurringExchangeKey: RecurringExchangeKey

  @Before
  fun setup() = runBlocking {
    val events = eventsOf("A" to "B", "C" to "D")
    val (encryptedEvents, dictionary) =
      preprocessEventsInPipeline(
        events,
        MAX_BYTE_SIZE,
        IDENTIFIER_HASH_PEPPER_PROVIDER,
        HKDF_PEPPER_PROVIDER,
        CRYPTO_KEY_PROVIDER,
        UncompressedDictionaryBuilder()
      )

    encr

    val databaseEntries = databaseEntries {
      encryptedEvents.
      entries += databaseEntry {

      }
    }

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

  // TODO: consider also adding a helper to write non-sharded files.
  protected fun <T : MessageLite> PCollection<T>.write(shardedFileName: ShardedFileName) {
    apply("Write ${shardedFileName.spec}", WriteShardedData(shardedFileName.spec, storageFactory))
  }

  @Test
  fun `entire process`() = runBlocking {
    //    logger.addToTaskLog(
    //      "Setup complete by creating a MP ${modelProviderKey} and an EDP ${dataProviderKey}."
    //    )
    val secretMap =
      TestSecretMap(
        mutableMapOf<String, ByteString>(
          Pair(recurringExchangeKey.recurringExchangeId, exchangeWorkflow.toByteString())
        )
      )

    val edpScope = CoroutineScope(CoroutineName("EDP SCOPE" + Dispatchers.Default))
    val mpScope = CoroutineScope(CoroutineName("MP SCOPE" + Dispatchers.Default))

    val edpStorageFactory = FileSystemStorageFactory(temporaryFolder.newFolder().absolutePath)
    val mpStorageFactory = FileSystemStorageFactory(temporaryFolder.newFolder().absolutePath)

    val edpInputBlobs =
      mapOf<String, ByteString>(
        "edp-hkdf-pepper" to ByteString.copyFromUtf8("hkdf-pepper"),
        "edp-identifier-hash-pepper" to ByteString.copyFromUtf8("identifier-hash-pepper"),
        "edp-commutative-deterministic-key" to ByteString.copyFromUtf8("edp-key"),
        "edp-previous-single-blinded-join-keys" to ByteString.EMPTY,
//        "edp-encrypted-event-data" to edpEncryptedEventData,
//        "edp-event-data-dictionary" to edpEventDataDictionary,
      )

    val mpInputBlobs =
      mapOf<String, ByteString>(
        "mp-join-keys" to ByteString.copyFromUtf8(""),
      )

    val edpStorage = edpStorageFactory.build()
    for ((k, v) in edpInputBlobs) {
      edpStorage.createBlob(k, v)
    }

    val mpStorage = mpStorageFactory.build()
    for ((k, v) in mpInputBlobs) {
      mpStorage.createBlob(k, v)
    }

    val edpDaemon =
      ExchangeWorkflowDaemonFromTest(
        channel = inProcessKingdom.publicApiChannel,
        providerKey = dataProviderKey,
        taskTimeoutDuration = Duration.ofSeconds(30),
        pollingInterval = Duration.ofSeconds(30),
        validExchangeWorkflow = secretMap,
        coroutineScope = edpScope,
        storageFactory = edpStorageFactory
      )
    val mpDaemon =
      ExchangeWorkflowDaemonFromTest(
        channel = inProcessKingdom.publicApiChannel,
        providerKey = modelProviderKey,
        taskTimeoutDuration = Duration.ofSeconds(30),
        pollingInterval = Duration.ofSeconds(30),
        validExchangeWorkflow = secretMap,
        coroutineScope = mpScope,
        storageFactory = mpStorageFactory
      )
    edpDaemon.run()
    mpDaemon.run()
    delay(Duration.ofMinutes(3).toMillis())
    edpScope.cancel()
    mpScope.cancel()

    val edpResponse = edpDaemon.apiClient.claimExchangeStep()
    val mpResponse = mpDaemon.apiClient.claimExchangeStep()

    assertThat(edpResponse).isNull()
    assertThat(mpResponse).isNull()
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
