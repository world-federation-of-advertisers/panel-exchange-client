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
import io.grpc.Channel
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Clock
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttemptsGrpcKt.ExchangeStepAttemptsCoroutineStub
import org.wfanet.measurement.api.v2alpha.ExchangeStepsGrpcKt.ExchangeStepsCoroutineStub
import org.wfanet.measurement.api.v2alpha.ResourceKey
import org.wfanet.measurement.common.asFlow
import org.wfanet.measurement.common.identity.withPrincipalName
import org.wfanet.measurement.common.throttler.MinimumIntervalThrottler
import org.wfanet.measurement.common.throttler.Throttler
import org.wfanet.measurement.common.toByteString
import org.wfanet.measurement.storage.StorageClient
import org.wfanet.panelmatch.client.common.Identity
import org.wfanet.panelmatch.client.deploy.ExchangeWorkflowDaemon
import org.wfanet.panelmatch.client.exchangetasks.ExchangeTaskMapper
import org.wfanet.panelmatch.client.launcher.ApiClient
import org.wfanet.panelmatch.client.launcher.GrpcApiClient
import org.wfanet.panelmatch.client.storage.StorageDetails
import org.wfanet.panelmatch.client.storage.StorageDetails.PlatformCase
import org.wfanet.panelmatch.client.storage.StorageDetailsKt.fileStorage
import org.wfanet.panelmatch.client.storage.StorageDetailsProvider
import org.wfanet.panelmatch.client.storage.StorageFactory
import org.wfanet.panelmatch.client.storage.storageDetails
import org.wfanet.panelmatch.common.ExchangeDateKey
import org.wfanet.panelmatch.common.Timeout
import org.wfanet.panelmatch.common.asTimeout
import org.wfanet.panelmatch.common.certificates.CertificateManager
import org.wfanet.panelmatch.common.certificates.testing.TestCertificateManager
import org.wfanet.panelmatch.common.loggerFor
import org.wfanet.panelmatch.common.secrets.SecretMap
import org.wfanet.panelmatch.common.secrets.testing.TestSecretMap
import org.wfanet.panelmatch.common.storage.createBlob
import org.wfanet.panelmatch.common.storage.toByteString

/** Executes ExchangeWorkflows for InProcess Integration testing. */
class ExchangeWorkflowDaemonForTest(
  v2alphaChannel: Channel,
  provider: ResourceKey,
  partnerProvider: ResourceKey,
  private val exchangeDateKey: ExchangeDateKey,
  serializedExchangeWorkflow: ByteString,
  privateDirectory: Path,
  sharedDirectory: Path,
  override val scope: CoroutineScope,
  override val clock: Clock = Clock.systemUTC(),
  pollingInterval: Duration = Duration.ofMillis(100),
  taskTimeoutDuration: Duration = Duration.ofMinutes(2),
) : ExchangeWorkflowDaemon() {
  private val recurringExchangeId = exchangeDateKey.recurringExchangeId

  override val certificateManager: CertificateManager = TestCertificateManager

  override val identity: Identity = Identity.fromResourceKey(provider)

  override val apiClient: ApiClient by lazy {
    val providerName = provider.toName()
    val exchangeStepsClient =
      ExchangeStepsCoroutineStub(v2alphaChannel).withPrincipalName(providerName)
    val exchangeStepAttemptsClient =
      ExchangeStepAttemptsCoroutineStub(v2alphaChannel).withPrincipalName(providerName)

    GrpcApiClient(identity, exchangeStepsClient, exchangeStepAttemptsClient, clock)
  }

  override val rootCertificates: SecretMap =
    TestSecretMap(
      partnerProvider.toName() to TestCertificateManager.CERTIFICATE.encoded.toByteString()
    )

  override val validExchangeWorkflows: SecretMap =
    TestSecretMap(recurringExchangeId to serializedExchangeWorkflow)

  override val throttler: Throttler = MinimumIntervalThrottler(clock, pollingInterval)

  override val exchangeTaskMapper: ExchangeTaskMapper by lazy {
    InProcessExchangeTaskMapper(
      privateStorageSelector = privateStorageSelector,
      sharedStorageSelector = sharedStorageSelector,
      certificateManager = certificateManager,
      inputTaskThrottler = MinimumIntervalThrottler(clock, Duration.ofMillis(250))
    )
  }

  override val privateStorageFactories:
    Map<PlatformCase, (StorageDetails, ExchangeDateKey) -> StorageFactory> =
    mapOf(PlatformCase.FILE to ::DemoFileSystemStorageFactory)

  private val privateStorageDetails = storageDetails {
    file = fileStorage { path = privateDirectory.toString() }
    visibility = StorageDetails.Visibility.PRIVATE
  }
  override val privateStorageInfo: StorageDetailsProvider =
    StorageDetailsProvider(
      TestSecretMap(recurringExchangeId to privateStorageDetails.toByteString())
    )

  /** Writes [contents] into private storage for an exchange. */
  fun writePrivateBlob(blobKey: String, contents: ByteString) =
    runBlocking(Dispatchers.IO) {
      privateStorageSelector.getStorageClient(exchangeDateKey).createBlob(blobKey, contents)
    }

  /** Reads a blob from private storage for an exchange. */
  fun readPrivateBlob(blobKey: String): ByteString? = runBlocking {
    privateStorageSelector.getStorageClient(exchangeDateKey).getBlob(blobKey)?.toByteString()
  }

  override val sharedStorageFactories:
    Map<PlatformCase, (StorageDetails, ExchangeDateKey) -> StorageFactory> =
    mapOf(PlatformCase.FILE to ::DemoFileSystemStorageFactory)

  private val sharedStorageDetails = storageDetails {
    file = fileStorage { path = sharedDirectory.toString() }
    visibility = StorageDetails.Visibility.SHARED
  }
  override val sharedStorageInfo: StorageDetailsProvider =
    StorageDetailsProvider(
      TestSecretMap(recurringExchangeId to sharedStorageDetails.toByteString())
    )

  override val taskTimeout: Timeout = taskTimeoutDuration.asTimeout()
}

private const val DEFAULT_BUFFER_SIZE_BYTES = 1024 * 4 // 4 KiB

private class DemoFileSystemStorageFactory(
  private val storageDetails: StorageDetails,
  private val exchangeDateKey: ExchangeDateKey
) : StorageFactory {
  override fun build(): StorageClient {
    val directory = Paths.get(storageDetails.file.path, exchangeDateKey.path).toFile()
    val absolutePath = directory.absolutePath
    if (directory.exists()) {
      logger.fine("Directory already exists: $absolutePath")
    } else {
      check(directory.mkdirs()) { "Unable to create recursively directory: $absolutePath" }
      logger.fine("Created directory: $absolutePath")
    }
    return DemoFileSystemStorageClient(directory)
  }

  companion object {
    private val logger by loggerFor()
  }
}

/** [StorageClient] implementation that utilizes flat files in the specified directory as blobs. */
private class DemoFileSystemStorageClient(private val directory: File) : StorageClient {
  init {
    require(directory.isDirectory) { "$directory is not a directory" }
  }

  override val defaultBufferSizeBytes: Int
    get() = DEFAULT_BUFFER_SIZE_BYTES

  override suspend fun createBlob(blobKey: String, content: Flow<ByteString>): StorageClient.Blob {
    val file = File(directory, blobKey)
    withContext(Dispatchers.IO) {
      try {
        File(file.parent).mkdirs()
        require(file.createNewFile()) { "$blobKey already exists" }
      } catch (e: Exception) {
        println("BLOB KEY $blobKey")
        throw e
      }

      file.outputStream().channel.use { byteChannel ->
        content.collect { bytes ->
          @Suppress("BlockingMethodInNonBlockingContext") // Flow context preservation.
          byteChannel.write(bytes.asReadOnlyByteBuffer())
        }
      }
    }

    return Blob(file)
  }

  override fun getBlob(blobKey: String): StorageClient.Blob? {
    val file = File(directory, blobKey)
    return if (file.exists()) Blob(file) else null
  }

  private inner class Blob(private val file: File) : StorageClient.Blob {
    override val storageClient: StorageClient
      get() = this@DemoFileSystemStorageClient

    override val size: Long
      get() = file.length()

    override fun read(bufferSizeBytes: Int): Flow<ByteString> =
      file.inputStream().channel.asFlow(bufferSizeBytes)

    override fun delete() {
      file.delete()
    }
  }
}
