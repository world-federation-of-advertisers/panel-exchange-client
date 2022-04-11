// Copyright 2022 The Cross-Media Measurement Authors
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

package org.wfanet.panelmatch.client.tools

import com.google.crypto.tink.integration.gcpkms.GcpKmsClient
import com.google.protobuf.kotlin.toByteString
import java.io.File
import java.time.LocalDate
import java.util.Optional
import java.util.concurrent.Callable
import kotlinx.coroutines.runBlocking
import org.wfanet.measurement.common.crypto.readCertificate
import org.wfanet.measurement.common.crypto.tink.TinkKeyStorageProvider
import org.wfanet.measurement.gcloud.gcs.GcsFromFlags
import org.wfanet.measurement.gcloud.gcs.GcsStorageClient
import org.wfanet.measurement.storage.StorageClient
import org.wfanet.panelmatch.client.deploy.DaemonStorageClientDefaults
import org.wfanet.panelmatch.client.storage.StorageDetails
import org.wfanet.panelmatch.client.storage.StorageDetailsKt
import org.wfanet.panelmatch.client.storage.gcloud.gcs.GcsStorageFactory
import org.wfanet.panelmatch.client.storage.storageDetails
import org.wfanet.panelmatch.common.ExchangeDateKey
import org.wfanet.panelmatch.common.storage.StorageFactory
import picocli.CommandLine

@CommandLine.Command(name = "add_all_resources", description = ["Adds all resources into GCS"])
class AddAllResources : Callable<Int> {

  @CommandLine.Option(
    names = ["--tink-key-uri"],
    description = ["URI for tink"],
    required = true,
  )
  private lateinit var tinkKeyUri: String

  @CommandLine.Mixin private lateinit var gcsFlags: GcsFromFlags.Flags

  @CommandLine.Option(
    names = ["--recurring-exchange-id"],
    description = ["API resource name of the recurring-exchange-id"],
    required = true,
  )
  private lateinit var recurringExchangeId: String

  @CommandLine.Option(
    names = ["--serialized-exchange-workflow-file"],
    description = ["Public API serialized ExchangeWorkflow"],
    required = true,
  )
  private lateinit var exchangeWorkflowFile: File

  @CommandLine.Option(
    names = ["--exchange-date"],
    description = ["Date in format of YYYY-MM-DD"],
    required = true,
  )
  private lateinit var exchangeDate: String

  @CommandLine.Option(
    names = ["--shared-storage-project"],
    description = ["Shared Storage Google Cloud Project name"],
    required = true,
  )
  private lateinit var sharedStorageProject: String

  @CommandLine.Option(
    names = ["--shared-storage-bucket"],
    description = ["Shared Storage Google Cloud Storage Bucket"],
    required = true,
  )
  private lateinit var sharedStorageBucket: String

  @CommandLine.Option(
    names = ["--partner-resource-name"],
    description = ["API partner resource name of the recurring exchange"],
    required = true,
  )
  private lateinit var partnerResourceName: String

  @CommandLine.Option(
    names = ["--partner-certificate-file"],
    description = ["Certificate for the principal"],
    required = true
  )
  private lateinit var partnerCertificateFile: File

  @CommandLine.Option(
    names = ["--workflow-input-blob-key"],
    description = ["Blob Key in Private Storage"],
    required = true,
  )
  private lateinit var workflowInputBlobKey: String

  @CommandLine.Option(
    names = ["--workflow-input-blob-contents"],
    description = ["Blob Contents"],
    required = true
  )
  private lateinit var workflowInputBlobContents: File

  private val rootStorageClient: StorageClient by lazy {
    GcsStorageClient.fromFlags(GcsFromFlags(gcsFlags))
  }

  private val defaults by lazy {
    // Register GcpKmsClient before setting storage folders.
    GcpKmsClient.register(Optional.of(tinkKeyUri), Optional.empty())
    DaemonStorageClientDefaults(rootStorageClient, tinkKeyUri, TinkKeyStorageProvider())
  }

  private val addResource by lazy { ConfigureResource(defaults) }

  private val privateStorageDetails by lazy {
    storageDetails {
      gcs =
        StorageDetailsKt.gcsStorage {
          projectName = gcsFlags.projectName
          bucket = gcsFlags.bucket
          bucketType = StorageDetails.BucketType.STATIC_BUCKET
        }
      visibility = StorageDetails.Visibility.PRIVATE
    }
  }

  private val sharedStorageDetails by lazy {
    storageDetails {
      gcs =
        StorageDetailsKt.gcsStorage {
          projectName = sharedStorageProject
          bucket = sharedStorageBucket
          bucketType = StorageDetails.BucketType.STATIC_BUCKET
        }
      visibility = StorageDetails.Visibility.SHARED
    }
  }

  /** This should be customized per deployment. */
  private val privateStorageFactory:
    Map<StorageDetails.PlatformCase, (StorageDetails, ExchangeDateKey) -> StorageFactory> by lazy {
    mapOf(StorageDetails.PlatformCase.GCS to ::GcsStorageFactory)
  }

  private val partnerCertificate by lazy { readCertificate(partnerCertificateFile) }

  override fun call(): Int {
    val serializedExchangeWorkflow = exchangeWorkflowFile.readBytes().toByteString()
    runBlocking {
      addResource.addWorkflow(serializedExchangeWorkflow, recurringExchangeId)
      addResource.addRootCertificates(partnerResourceName, partnerCertificate)
      addResource.addPrivateStorageInfo(recurringExchangeId, privateStorageDetails)
      addResource.addSharedStorageInfo(recurringExchangeId, sharedStorageDetails)
      addResource.provideWorkflowInput(
        recurringExchangeId,
        LocalDate.parse(exchangeDate),
        privateStorageFactory,
        workflowInputBlobKey,
        workflowInputBlobContents.readBytes().toByteString()
      )
    }
    return 0
  }
}