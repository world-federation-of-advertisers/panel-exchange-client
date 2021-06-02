package org.wfanet.panelmatch.client.deploy

import picocli.CommandLine
import java.time.Duration

class ExchangeWorkflowFlags {
  @CommandLine.Option(
    names = ["--id"],
    description = ["Id of the provider"],
    required = true
  )
  lateinit var id: String
    private set

  @CommandLine.Option(
    names = ["--party-type"],
    description = ["Type of the party [model, data]."],
    defaultValue = "model",
    required = true
  )
  lateinit var partyType: String
    private set

  @CommandLine.Option(
    names = ["--channel-shutdown-timeout"],
    defaultValue = "3s",
    description = ["How long to allow for the gRPC channel to shutdown."],
    required = true
  )
  lateinit var channelShutdownTimeout: Duration
    private set

  @CommandLine.Option(
    names = ["--polling-interval"],
    defaultValue = "1m",
    description = ["How long to sleep between finding and running an ExchangeStep."],
    required = true
  )
  lateinit var pollingInterval: Duration
    private set

  @CommandLine.Option(
    names = ["--exchange-steps-service-target"],
    description = ["Address and port of the Exchange Steps Service"],
    required = true
  )
  lateinit var exchangeStepsServiceTarget: String
    private set

  @CommandLine.Option(
    names = ["--exchange-step-attempts-service-target"],
    description = ["Address and port of the Exchange Step Attempts service"],
    required = true
  )
  lateinit var exchangeStepAttemptsServiceTarget: String
    private set
}
