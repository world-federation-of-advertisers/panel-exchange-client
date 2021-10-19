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

import io.grpc.Channel
import java.security.cert.X509Certificate
import java.time.Clock
import java.time.Duration
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttemptsGrpcKt.ExchangeStepAttemptsCoroutineStub
import org.wfanet.measurement.api.v2alpha.ExchangeStepsGrpcKt.ExchangeStepsCoroutineStub
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.measurement.common.throttler.MinimumIntervalThrottler
import org.wfanet.measurement.common.throttler.Throttler
import org.wfanet.panelmatch.client.launcher.ApiClient
import org.wfanet.panelmatch.client.launcher.GrpcApiClient
import org.wfanet.panelmatch.client.launcher.Identity
import org.wfanet.panelmatch.client.storage.VerifiedStorageClient
import org.wfanet.panelmatch.common.Timeout
import org.wfanet.panelmatch.common.asTimeout
import org.wfanet.panelmatch.common.secrets.SecretMap

/** Executes ExchangeWorkflows for InProcess Integration testing. */
class ExchangeWorkflowDaemonFromTest(
  private val channel: Channel,
  private val providerId: String,
  private val providerType: ExchangeWorkflow.Party,
  private val taskTimeoutDuration: Duration,
  private val pollingInterval: Duration,
  private val validExchangeWorkflow: SecretMap
) : ExchangeWorkflowDaemon() {

  override val privateStorage: VerifiedStorageClient
    get() = TODO("Not yet implemented")
  override val localCertificate: X509Certificate
    get() = TODO("Not yet implemented")
  override val uriPrefix: String
    get() = TODO("Not yet implemented: coming from client storage")

  override val validExchangeWorkflows: SecretMap by lazy {
    validExchangeWorkflow
  }

  override val apiClient: ApiClient by lazy {
    val exchangeStepsClient = ExchangeStepsCoroutineStub(channel)

    val exchangeStepAttemptsClient = ExchangeStepAttemptsCoroutineStub(channel)

    GrpcApiClient(
      Identity(providerId, providerType),
      exchangeStepsClient,
      exchangeStepAttemptsClient,
      Clock.systemUTC()
    )
  }

  override val throttler: Throttler by lazy {
    MinimumIntervalThrottler(Clock.systemUTC(), pollingInterval)
  }

  override val taskTimeout: Timeout by lazy { taskTimeoutDuration.asTimeout() }

  override val identity: Identity by lazy { Identity(providerId, providerType) }
}
