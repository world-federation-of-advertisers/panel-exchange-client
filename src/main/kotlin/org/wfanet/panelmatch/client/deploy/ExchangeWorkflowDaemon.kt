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
import org.wfanet.panelmatch.client.exchangetasks.ExchangeTaskMapper
import org.wfanet.panelmatch.client.launcher.ApiClient
import org.wfanet.panelmatch.client.launcher.CoroutineLauncher
import org.wfanet.panelmatch.client.launcher.ExchangeStepLauncher
import org.wfanet.panelmatch.client.launcher.ExchangeStepValidatorImpl
import org.wfanet.panelmatch.client.launcher.ExchangeTaskExecutor
import org.wfanet.panelmatch.client.launcher.Identity
import org.wfanet.panelmatch.client.storage.StorageFactory
import org.wfanet.panelmatch.common.Timeout
import org.wfanet.panelmatch.common.secrets.SecretMap

/** Runs ExchangeWorkflows. */
abstract class ExchangeWorkflowDaemon : Runnable {

  /** Identity of the party executing this daemon. */
  abstract val identity: Identity

  /** Kingdom [ApiClient]. */
  abstract val apiClient: ApiClient

  /** [StorageFactory] for writing to local (non-shared) storage. */
  abstract val privateStorageFactory: StorageFactory

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

  override fun run() {

    val stepExecutor =
      ExchangeTaskExecutor(
        apiClient = apiClient,
        timeout = taskTimeout,
        privateStorage = privateStorageFactory.build(),
        getExchangeTaskForStep = exchangeTaskMapper::getExchangeTaskForStep
      )

    val launcher = CoroutineLauncher(stepExecutor = stepExecutor)

    val exchangeStepLauncher =
      ExchangeStepLauncher(
        apiClient = apiClient,
        validator = ExchangeStepValidatorImpl(identity.party, validExchangeWorkflows, clock),
        jobLauncher = launcher
      )

    scope.launch(CoroutineName("ExchangeWorkflowDaemon")) {
      throttler.loopOnReady {
        logAndSuppressExceptionSuspend { exchangeStepLauncher.findAndRunExchangeStep() }
      }
    }
  }
}
