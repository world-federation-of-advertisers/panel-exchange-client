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

package org.wfanet.panelmatch.client.tools.gcloud

import com.google.crypto.tink.integration.gcpkms.GcpKmsClient
import java.util.Optional
import org.wfanet.measurement.common.crypto.tink.TinkKeyStorageProvider
import org.wfanet.measurement.gcloud.gcs.GcsFromFlags
import org.wfanet.measurement.gcloud.gcs.GcsStorageClient
import org.wfanet.measurement.storage.StorageClient
import org.wfanet.panelmatch.client.deploy.DaemonStorageClientDefaults
import org.wfanet.panelmatch.client.storage.StorageDetails
import org.wfanet.panelmatch.client.storage.StorageDetails.PlatformCase
import org.wfanet.panelmatch.client.storage.gcloud.gcs.GcsStorageFactory
import org.wfanet.panelmatch.client.tools.ConfigureResource
import org.wfanet.panelmatch.common.ExchangeDateKey
import org.wfanet.panelmatch.common.storage.StorageFactory
import picocli.CommandLine

class GoogleCloudStorageFlags {
  @CommandLine.Mixin private lateinit var gcsFlags: GcsFromFlags.Flags

  @CommandLine.Option(
    names = ["--tink-key-uri"],
    description = ["URI for tink"],
    required = true,
  )
  private lateinit var tinkKeyUri: String

  private val rootStorageClient: StorageClient by lazy {
    GcsStorageClient.fromFlags(GcsFromFlags(gcsFlags))
  }

  private val defaults by lazy {
    // Register GcpKmsClient before setting storage folders. Set GOOGLE_APPLICATION_CREDENTIALS.
    GcpKmsClient.register(Optional.of(tinkKeyUri), Optional.empty())
    DaemonStorageClientDefaults(rootStorageClient, tinkKeyUri, TinkKeyStorageProvider())
  }

  val addResource by lazy { ConfigureResource(defaults) }

  /** This should be customized per deployment. */
  val privateStorageFactories:
    Map<PlatformCase, (StorageDetails, ExchangeDateKey) -> StorageFactory> =
    mapOf(PlatformCase.GCS to ::GcsStorageFactory)
}
