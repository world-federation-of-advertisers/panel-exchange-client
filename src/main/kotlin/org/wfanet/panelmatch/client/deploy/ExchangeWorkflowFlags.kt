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

import java.net.InetSocketAddress
import java.time.Duration
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow.Party
import org.wfanet.measurement.common.grpc.TlsFlags
import org.wfanet.panelmatch.client.eventpreprocessing.PreprocessingStepContext
import picocli.CommandLine
import picocli.CommandLine.ITypeConverter
import picocli.CommandLine.Option
import picocli.CommandLine.TypeConversionException

class ExchangeWorkflowFlags {
  @Option(names = ["--id"], description = ["Id of the provider"], required = true)
  lateinit var id: String
    private set

  @Option(
    names = ["--party-type"],
    description = ["Type of the party: \${COMPLETION-CANDIDATES}"],
    required = true
  )
  lateinit var partyType: Party
    private set

  @CommandLine.Mixin
  lateinit var tlsFlags: TlsFlags
    private set

  @Option(
    names = ["--channel-shutdown-timeout"],
    defaultValue = "3s",
    description = ["How long to allow for the gRPC channel to shutdown."],
    required = true
  )
  lateinit var channelShutdownTimeout: Duration
    private set

  @Option(
    names = ["--storage-signing-algorithm"],
    defaultValue = "EC",
    description = ["The algorithm used in signing data written to shared storage."],
    required = true
  )
  lateinit var certAlgorithm: String
    private set

  @Option(
    names = ["--polling-interval"],
    defaultValue = "1m",
    description = ["How long to sleep between finding and running an ExchangeStep."],
    required = true
  )
  lateinit var pollingInterval: Duration
    private set

  @Option(
    names = ["--task-timeout"],
    defaultValue = "24h",
    description = ["How long to sleep between finding and running an ExchangeStep."],
    required = true
  )
  lateinit var taskTimeout: Duration
    private set

  @Option(
    names = ["--exchange-api-target"],
    description =
      ["Address and port for servers hosting /ExchangeSteps and /ExchangeStepAttempts services"],
    converter = [InetSocketAddressConverter::class],
    required = true
  )
  lateinit var exchangeApiTarget: InetSocketAddress
    private set

  @Option(
    names = ["--exchange-api-cert-host"],
    description = ["Expected hostname in the TLS certificate for --exchange-api-target"],
    required = true
  )
  lateinit var exchangeApiCertHost: String
    private set

  @CommandLine.Mixin
  lateinit var preprocessingStepContext: PreprocessingStepContext
    private set

  @Option(
    names = ["--max-byte-size"],
    description = ["Max batch size for processing"],
    required = true
  )
  lateinit var dataProviderMaxByteSize: String
    private set

  @Option(
    names = ["--file-count"],
    description = ["Number of output files from event preprocessing step"],
    required = true
  )
  lateinit var fileCount: String
    private set

  private inner class InetSocketAddressConverter : ITypeConverter<InetSocketAddress> {
    override fun convert(value: String): InetSocketAddress {
      val pos = value.lastIndexOf(':')
      if (pos < 0) {
        throw TypeConversionException("Invalid format: must be 'host:port' but was '$value'")
      }
      val adr = value.substring(0, pos)
      val port = value.substring(pos + 1).toInt() // invalid port shows the generic error message
      return InetSocketAddress(adr, port)
    }
  }
}
