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
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wfanet.measurement.common.logAndSuppressExceptionSuspend
import org.wfanet.measurement.common.throttler.Throttler
import org.wfanet.panelmatch.client.common.Identity
import org.wfanet.panelmatch.client.exchangetasks.ExchangeTaskMapper
import org.wfanet.panelmatch.client.launcher.ApiClient
import org.wfanet.panelmatch.client.launcher.CoroutineLauncher
import org.wfanet.panelmatch.client.launcher.ExchangeStepManager
import org.wfanet.panelmatch.client.launcher.ExchangeStepReporter
import org.wfanet.panelmatch.client.launcher.ExchangeStepValidatorImpl
import org.wfanet.panelmatch.client.launcher.ExchangeTaskExecutor
import org.wfanet.panelmatch.client.storage.PrivateStorageSelector
import org.wfanet.panelmatch.client.storage.SharedStorageSelector
import org.wfanet.panelmatch.client.storage.StorageDetails
import org.wfanet.panelmatch.client.storage.StorageDetails.PlatformCase
import org.wfanet.panelmatch.client.storage.StorageDetailsProvider
import org.wfanet.panelmatch.common.ExchangeDateKey
import org.wfanet.panelmatch.common.Timeout
import org.wfanet.panelmatch.common.certificates.CertificateManager
import org.wfanet.panelmatch.common.secrets.SecretMap
import org.wfanet.panelmatch.common.storage.StorageFactory

/** Runs ExchangeWorkflows. */
abstract class ExchangeWorkflowDaemon : Runnable {
  /** Identity of the party executing this daemon. */
  abstract val identity: Identity

  /** Kingdom [ApiClient]. */
  abstract val apiClient: ApiClient

  /**
   * Maps partner resource names to serialized root certificates (from `X509Certificate::encoded').
   */
  abstract val rootCertificates: SecretMap

  /** [SecretMap] from RecurringExchange ID to serialized ExchangeWorkflow. */
  abstract val validExchangeWorkflows: SecretMap

  /** Limits how often to poll. */
  abstract val throttler: Throttler

  /** How long a task should be allowed to run for before being cancelled. */
  abstract val taskTimeout: Timeout

  /** [ExchangeTaskMapper] to create a task based on the step */
  abstract val exchangeTaskMapper: ExchangeTaskMapper

  /** Clock for ensuring future Exchanges don't execute yet. */
  abstract val clock: Clock

  /** Scope in which to run the daemon loop. */
  abstract val scope: CoroutineScope

  /** How to build private storage. */
  protected abstract val privateStorageFactories:
    Map<PlatformCase, (StorageDetails, ExchangeDateKey) -> StorageFactory>

  /** Serialized [StorageDetails] per RecurringExchange. */
  protected abstract val privateStorageInfo: StorageDetailsProvider

  /** How to build shared storage. */
  protected abstract val sharedStorageFactories:
    Map<PlatformCase, (StorageDetails, ExchangeDateKey) -> StorageFactory>

  /** Serialized [StorageDetails] per RecurringExchange. */
  protected abstract val sharedStorageInfo: StorageDetailsProvider

  /** [CertificateManager] for [sharedStorageSelector]. */
  protected abstract val certificateManager: CertificateManager

  /** [PrivateStorageSelector] for writing to local (non-shared) storage. */
  protected val privateStorageSelector: PrivateStorageSelector by lazy {
    PrivateStorageSelector(privateStorageFactories, privateStorageInfo)
  }

  /** [SharedStorageSelector] for writing to shared storage. */
  protected val sharedStorageSelector: SharedStorageSelector by lazy {
    SharedStorageSelector(certificateManager, sharedStorageFactories, sharedStorageInfo)
  }

  /** Generates of fetches a jobId for each task run. */
  protected abstract val generateJobId: () -> String

  /** Reports the status of exchange claiming and execution. */
  protected abstract val exchangeStepReporter: ExchangeStepReporter

  override fun run() {

    val stepExecutor =
      ExchangeTaskExecutor(
        timeout = taskTimeout,
        privateStorageSelector = privateStorageSelector,
        exchangeTaskMapper = exchangeTaskMapper,
        exchangeStepReporter = exchangeStepReporter,
      )

    val launcher = CoroutineLauncher(stepExecutor = stepExecutor)

    val validator = ExchangeStepValidatorImpl(identity.party, validExchangeWorkflows, clock)

    val exchangeStepManager =
      ExchangeStepManager(
        validator = validator,
        jobLauncher = launcher,
        exchangeStepReporter = exchangeStepReporter,
        generateJobId = generateJobId,
      )

    scope.launch(CoroutineName("ExchangeWorkflowDaemon")) {
      throttler.loopOnReady { logAndSuppressExceptionSuspend { exchangeStepManager.manageStep() } }
    }
  }
}
