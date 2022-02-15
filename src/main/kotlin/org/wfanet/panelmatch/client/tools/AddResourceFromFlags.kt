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

package org.wfanet.panelmatch.client.tools

import com.google.privatemembership.batch.Shared
import com.google.protobuf.TypeRegistry
import com.google.protobuf.kotlin.toByteString
import java.io.File
import java.nio.file.Files
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.Callable
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking
import org.wfanet.measurement.api.v2alpha.copy
import org.wfanet.measurement.api.v2alpha.exchangeWorkflow
import org.wfanet.measurement.common.crypto.readCertificate
import org.wfanet.measurement.common.crypto.tink.TinkKeyStorageProvider
import org.wfanet.measurement.common.parseTextProto
import org.wfanet.measurement.common.toProtoDate
import org.wfanet.measurement.storage.StorageClient
import org.wfanet.measurement.storage.filesystem.FileSystemStorageClient
import org.wfanet.panelmatch.client.deploy.DaemonStorageClientDefaults
import org.wfanet.panelmatch.client.storage.FileSystemStorageFactory
import org.wfanet.panelmatch.client.storage.StorageDetails
import org.wfanet.panelmatch.client.storage.StorageDetails.PlatformCase
import org.wfanet.panelmatch.client.storage.storageDetails
import org.wfanet.panelmatch.common.ExchangeDateKey
import org.wfanet.panelmatch.common.storage.StorageFactory
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.HelpCommand
import picocli.CommandLine.Mixin
import picocli.CommandLine.Option

private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d/MM/yyyy")

class AddResourceFlags {
  @Option(
    names = ["--private-storage-root"],
    description = ["Private storage root directory"],
    required = true
  )
  private lateinit var privateStorageRoot: File

  @Option(
    names = ["--tink-key-uri"],
    description = ["URI for tink"],
    required = true,
  )
  private lateinit var tinkKeyUri: String

  /** This should be customized per deployment. */
  private val rootStorageClient: StorageClient by lazy {
    require(privateStorageRoot.exists() && privateStorageRoot.isDirectory)
    FileSystemStorageClient(privateStorageRoot)
  }

  /** This should be customized per deployment. */
  private val defaults by lazy {
    DaemonStorageClientDefaults(rootStorageClient, tinkKeyUri, ::TinkKeyStorageProvider)
  }

  val addResource by lazy { AddResource(defaults) }

  /** This should be customized per deployment. */
  val privateStorageFactories:
    Map<PlatformCase, (StorageDetails, ExchangeDateKey) -> StorageFactory> =
    mapOf(PlatformCase.FILE to ::FileSystemStorageFactory)
}

@Command(name = "add_workflow", description = ["Creates a DataProvider"])
private class AddWorkflowCommand : Callable<Int> {

  @Mixin private lateinit var flags: AddResourceFlags

  @Option(
    names = ["--recurring-exchange-id"],
    description = ["API resource name of the recurring-exchange-id"],
    required = true,
  )
  private lateinit var recurringExchangeId: String

  @Option(
    names = ["--exchange-workflow-file"],
    description = ["Public API serialized ExchangeWorkflow"],
    required = true,
  )
  private lateinit var exchangeWorkflowFile: File

  @Option(
    names = ["--start-date"],
    description = ["Date in format of d/MM/YYYY"],
    required = true,
  )
  private lateinit var startDate: String

  private val firstExchange by lazy { LocalDate.parse(startDate, formatter) }

  override fun call(): Int {
    val typeRegistry = TypeRegistry.newBuilder().add(Shared.Parameters.getDescriptor()).build()
    val exchangeWorkflow =
      checkNotNull(Files.newInputStream(exchangeWorkflowFile.toPath()))
        .use { input -> parseTextProto(input.bufferedReader(), exchangeWorkflow {}, typeRegistry) }
        .copy { this.firstExchangeDate = firstExchange.toProtoDate() }
    runBlocking { flags.addResource.addWorkflow(exchangeWorkflow, recurringExchangeId) }
    return 0
  }
}

@Command(name = "add_root_certificate", description = ["Adds a Root Certificate for another party"])
private class AddRootCertificateCommand : Callable<Int> {

  @Mixin private lateinit var flags: AddResourceFlags

  @Option(
    names = ["--partner-id"],
    description = ["API resource name of the recurring-exchange-id"],
    required = true,
  )
  private lateinit var partnerId: String

  @Option(
    names = ["--certificate-der-file"],
    description = ["Certificate for the principal"],
    required = true
  )
  private lateinit var certificateFile: File

  private val certificate by lazy { readCertificate(certificateFile) }

  override fun call(): Int {
    runBlocking { flags.addResource.addRootCertificates(partnerId, certificate) }
    return 0
  }
}

@Command(name = "add_private_storage_info", description = ["Adds Private Storage Info"])
private class AddPrivateStorageInfoCommand : Callable<Int> {

  @Mixin private lateinit var flags: AddResourceFlags

  @Option(
    names = ["--recurring-exchange-id"],
    description = ["API resource name of the recurring-exchange-id"],
    required = true,
  )
  private lateinit var recurringExchangeId: String

  @Option(
    names = ["--private-storage-info-file"],
    description = ["Private Storage Info"],
    required = true,
  )
  private lateinit var privateStorageInfo: File

  override fun call(): Int {
    val storageDetails =
      checkNotNull(Files.newInputStream(privateStorageInfo.toPath())).use { input ->
        parseTextProto(input.bufferedReader(), storageDetails {})
      }
    runBlocking { flags.addResource.addPrivateStorageInfo(recurringExchangeId, storageDetails) }
    return 0
  }
}

@Command(name = "add_shared_storage_info", description = ["Adds Shared Storage Info"])
private class AddSharedStorageInfoCommand : Callable<Int> {

  @Mixin private lateinit var flags: AddResourceFlags

  @Option(
    names = ["--recurring-exchange-id"],
    description = ["API resource name of the recurring-exchange-id"],
    required = true,
  )
  private lateinit var recurringExchangeId: String

  @Option(
    names = ["--shared-storage-info-file"],
    description = ["Shared Storage Info"],
    required = true,
  )
  private lateinit var sharedStorageInfo: File

  override fun call(): Int {
    val storageDetails =
      checkNotNull(Files.newInputStream(sharedStorageInfo.toPath())).use { input ->
        parseTextProto(input.bufferedReader(), storageDetails {})
      }
    runBlocking { flags.addResource.addSharedStorageInfo(recurringExchangeId, storageDetails) }
    return 0
  }
}

@Command(
  name = "provide_workflow_input",
  description = ["Will copy workflow input to private storage"]
)
private class ProvideWorkflowInputCommand : Callable<Int> {

  @Mixin private lateinit var flags: AddResourceFlags

  @Option(
    names = ["--recurring-exchange-id"],
    description = ["API resource name of the recurring-exchange-id"],
    required = true,
  )
  private lateinit var recurringExchangeId: String

  @Option(
    names = ["--exchange-date"],
    description = ["Date in format of d/MM/YYYY"],
    required = true,
  )
  private lateinit var exchangeDate: String

  @Option(
    names = ["--blob-key"],
    description = ["Blob Key in Private Storage"],
    required = true,
  )
  private lateinit var blobKey: String

  @Option(names = ["--blob-contents"], description = ["Blob Contents"], required = true)
  private lateinit var blobContents: File

  override fun call(): Int {
    runBlocking {
      flags.addResource.provideWorkflowInput(
        recurringExchangeId,
        LocalDate.parse(exchangeDate, formatter),
        flags.privateStorageFactories,
        blobKey,
        blobContents.readBytes().toByteString()
      )
    }
    return 0
  }
}

@Command(
  name = "create_resource",
  description = ["Creates resources for panel client"],
  subcommands =
    [
      HelpCommand::class,
      AddWorkflowCommand::class,
      AddRootCertificateCommand::class,
      AddPrivateStorageInfoCommand::class,
      AddSharedStorageInfoCommand::class,
      ProvideWorkflowInputCommand::class,
    ]
)
class CreateResource : Callable<Int> {
  /** Return 0 for success -- all work happens in subcommands. */
  override fun call(): Int = 0
}

/**
 * Creates resources for each client
 *
 * Use the `help` command to see usage details:
 *
 * ```
 * $ bazel build //src/main/kotlin/org/wfanet/panelmatch/client/tools:create_resource
 * $ bazel-bin/src/main/kotlin/org/wfanet/panelmatch/client/tools/create_resource help
 * Usage: create_resource [COMMAND]
 * Creates resources for each client
 * Commands:
 *  help                              Displays help information about the specified command
 *  add_workflow                      Adds a workflow
 *  add_root_certificate              Adds root certificate for another party
 *  add_shared_storage_info           Add shared storage info
 *  add_private_storage_info          Add private storage info
 *  provide_workflow_input            Adds a workflow
 * ```
 */
fun main(args: Array<String>) {
  exitProcess(CommandLine(CreateResource()).execute(*args))
}
