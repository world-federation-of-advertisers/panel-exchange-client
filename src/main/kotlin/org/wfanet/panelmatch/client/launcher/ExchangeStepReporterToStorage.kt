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

package org.wfanet.panelmatch.client.launcher

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttempt
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttemptKey
import org.wfanet.measurement.common.throttler.Throttler
import org.wfanet.measurement.storage.StorageClient
import org.wfanet.panelmatch.common.storage.createOrReplaceBlob
import org.wfanet.panelmatch.common.storage.toByteString
import org.wfanet.panelmatch.protocol.ClaimedExchangeStep
import org.wfanet.panelmatch.protocol.ExchangeStepStatus
import org.wfanet.panelmatch.protocol.exchangeStepStatus

class ExchangeStepReporterToStorage(
  private val apiClient: ApiClient,
  private val storageClient: StorageClient,
  private val statusThrottler: Throttler
) : ExchangeStepReporter {

  override suspend fun getAndStoreClaimStatus(
    jobId: String,
  ): ClaimedExchangeStep? {
    val claimedStep = apiClient.claimExchangeStep() ?: return null
    storageClient.createOrReplaceBlob(jobId, claimedStep.toByteString())
    return claimedStep
  }

  override suspend fun getClaimStatus(
    jobId: String,
  ): ClaimedExchangeStep {
    print("GET C STATUS!!!\n\n")
    return ClaimedExchangeStep.parseFrom(storageClient.getBlob(jobId)!!.toByteString())
  }

  override suspend fun storeExecutionStatus(
    jobId: String,
    key: ExchangeStepAttemptKey,
    state: ExchangeStepAttempt.State,
    logEntryMessages: Iterable<String>
  ) {
    val attemptStatus = exchangeStepStatus {
      this.key = key.toName()
      this.state = state
      logs += logEntryMessages
    }
    storageClient.createOrReplaceBlob(jobId, attemptStatus.toByteString())
  }

  override suspend fun reportExecutionStatus(jobId: String) {
    CoroutineScope(Dispatchers.Default).launch {
      var executionStatus: ExchangeStepStatus? = null
      while (executionStatus == null) {
        if (statusThrottler.onReady { getExecutionStatus(jobId) != null }) {
          executionStatus = getExecutionStatus(jobId)!!
        }
      }
      apiClient.reportStepAttempt(
        ExchangeStepAttemptKey.fromName(executionStatus.key)!!,
        executionStatus.state,
        executionStatus.logsList
      )
    }
  }

  private suspend fun getExecutionStatus(jobId: String): ExchangeStepStatus? {
    return try {
      ExchangeStepStatus.parseFrom(storageClient.getBlob(jobId)!!.toByteString())
    } catch (_: Exception) {
      null
    }
  }
}
