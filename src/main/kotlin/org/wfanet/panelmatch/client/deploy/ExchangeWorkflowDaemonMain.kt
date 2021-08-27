package org.wfanet.panelmatch.client.deploy

import org.wfanet.measurement.common.commandLineMain
import org.wfanet.measurement.storage.StorageClient
import picocli.CommandLine

@CommandLine.Command(
  name = "ExchangeWorkflowDaemon",
  description = ["Daemon for executing ExchangeWorkflows"],
  mixinStandardHelpOptions = true,
  showDefaultValues = true
)
private object UnimplementedExchangeWorkflowDaemon : ExchangeWorkflowDaemon() {
  override val sharedStorage: StorageClient
    get() = TODO("Not yet implemented")
  override val privateStorage: StorageClient
    get() = TODO("Not yet implemented")
}

/**
 * Reference implementation of a daemon for executing Exchange Workflows.
 *
 * TODO(@jonmolle): implement the proper [StorageClient]s and any flags to support them.
 */
fun main(args: Array<String>) = commandLineMain(UnimplementedExchangeWorkflowDaemon, args)
