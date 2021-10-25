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

package org.wfanet.panelmatch.client.deploy

import java.time.Clock
import org.wfanet.measurement.common.commandLineMain
import org.wfanet.panelmatch.client.common.ExchangeContext
import org.wfanet.panelmatch.client.storage.FileSystemStorageFactory
import org.wfanet.panelmatch.client.storage.GcsStorageFactory
import org.wfanet.panelmatch.client.storage.StorageDetails
import org.wfanet.panelmatch.client.storage.StorageDetails.PlatformCase
import org.wfanet.panelmatch.client.storage.StorageFactory
import org.wfanet.panelmatch.common.ExchangeDateKey
import org.wfanet.panelmatch.common.certificates.CertificateAuthority
import org.wfanet.panelmatch.common.secrets.MutableSecretMap
import org.wfanet.panelmatch.common.secrets.SecretMap
import picocli.CommandLine

@CommandLine.Command(
  name = "ExchangeWorkflowDaemonFromFlags",
  description = ["Daemon for executing ExchangeWorkflows"],
  mixinStandardHelpOptions = true,
  showDefaultValues = true
)
private object UnimplementedExchangeWorkflowDaemon : ExchangeWorkflowDaemonFromFlags() {
  @CommandLine.Mixin
  lateinit var approvedWorkflowFlags: PlaintextApprovedWorkflowFileFlags
    private set

  @CommandLine.Mixin
  lateinit var blobSizeFlags: BlobSizeFlags
    private set

  // TODO: All SecretMaps should be implemented with a static storageClient- or KMS-backed SecretMap
  //  based on flags. To start this should support our existing StorageFactory implementations
  //  and call a helper function to choose based on storage details (similar to
  //  privateStorageSelector, which we unfortunately can't use here as it depends on some of these.)
  override val privateKeys: MutableSecretMap
    get() = TODO("Not yet implemented")

  override val rootCertificates: SecretMap
    get() = TODO("Not yet implemented")

  // This is the most likely piece of storage to be customized, as it gives a party control over
  // what StorageClient types they actually support for shared and private storage. That's the main
  // reason it has been pushed all the way out here for implementation: we expect most custom main
  // file implementations to want to specify these.
  override val privateStorageFactories:
    Map<PlatformCase, ExchangeDateKey.(StorageDetails) -> StorageFactory> =
    mapOf(PlatformCase.FILE to { storageDetails -> FileSystemStorageFactory(storageDetails, this) })

  override val privateStorageInfo: SecretMap
    get() = TODO("Not yet implemented")

  override val sharedStorageFactories:
    Map<PlatformCase, ExchangeContext.(StorageDetails) -> StorageFactory> =
    mapOf(
      PlatformCase.GCS to
        { storageDetails ->
          blobSizeFlags.wrapStorageFactory(GcsStorageFactory(storageDetails, exchangeDateKey))
        }
    )

  override val sharedStorageInfo: SecretMap
    get() = TODO("Not yet implemented")

  override val clock: Clock = Clock.systemUTC()

  override val certificateAuthority: CertificateAuthority
    get() = TODO("Not yet implemented")

  override val validExchangeWorkflows: SecretMap by lazy {
    approvedWorkflowFlags.approvedExchangeWorkflows
  }
}

/** Reference implementation of a daemon for executing Exchange Workflows. */
fun main(args: Array<String>) = commandLineMain(UnimplementedExchangeWorkflowDaemon, args)
