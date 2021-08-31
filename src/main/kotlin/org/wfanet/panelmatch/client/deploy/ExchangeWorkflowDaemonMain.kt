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

import org.wfanet.measurement.common.commandLineMain
import org.wfanet.panelmatch.client.storage.VerifiedStorageClient
import picocli.CommandLine

@CommandLine.Command(
  name = "ExchangeWorkflowDaemon",
  description = ["Daemon for executing ExchangeWorkflows"],
  mixinStandardHelpOptions = true,
  showDefaultValues = true
)
private object UnimplementedExchangeWorkflowDaemon : ExchangeWorkflowDaemon() {
  override val sharedStorage: VerifiedStorageClient
    get() = TODO("Not yet implemented")
  override val privateStorage: VerifiedStorageClient
    get() = TODO("Not yet implemented")
}

/**
 * Reference implementation of a daemon for executing Exchange Workflows.
 *
 * TODO(@jonmolle): implement the proper [VerifiedStorageClient]s and any flags to support them.
 */
fun main(args: Array<String>) = commandLineMain(UnimplementedExchangeWorkflowDaemon, args)
