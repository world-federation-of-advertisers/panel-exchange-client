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

import com.google.protobuf.Timestamp
import java.time.Clock
import java.time.Instant
import org.wfanet.measurement.api.v2alpha.AppendLogEntryRequest
import org.wfanet.measurement.api.v2alpha.ClaimReadyExchangeStepRequest
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttempt
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttemptsGrpcKt.ExchangeStepAttemptsCoroutineStub
import org.wfanet.measurement.api.v2alpha.ExchangeStepsGrpcKt.ExchangeStepsCoroutineStub
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow.Party
import org.wfanet.measurement.api.v2alpha.FinishExchangeStepAttemptRequest
import org.wfanet.panelmatch.client.launcher.ApiClient.ClaimedExchangeStep

class GrpcApiClient(
  private val identity: Identity,
  private val exchangeStepsClient: ExchangeStepsCoroutineStub,
  private val exchangeStepAttemptsClient: ExchangeStepAttemptsCoroutineStub,
  private val clock: Clock = Clock.systemUTC()
) : ApiClient {
  private val claimReadyExchangeStepRequest =
    ClaimReadyExchangeStepRequest.newBuilder()
      .apply {
        when (identity.party) {
          Party.DATA_PROVIDER -> dataProviderBuilder.dataProviderId = identity.id
          Party.MODEL_PROVIDER -> modelProviderBuilder.modelProviderId = identity.id
          else -> error("Invalid Identity: $identity")
        }
      }
      .build()

  override suspend fun claimExchangeStep(): ClaimedExchangeStep? {
    val response = exchangeStepsClient.claimReadyExchangeStep(claimReadyExchangeStepRequest)
    if (response.hasExchangeStep()) {
      return ClaimedExchangeStep(response.exchangeStep, response.exchangeStepAttempt)
    }
    return null
  }

  override suspend fun appendLogEntry(key: ExchangeStepAttempt.Key, vararg messages: String) {
    val request =
      AppendLogEntryRequest.newBuilder()
        .also {
          it.key = key
          it.addAllLogEntries(messages.map(this::makeLogEntry))
        }
        .build()
    exchangeStepAttemptsClient.appendLogEntry(request)
  }

  override suspend fun finishExchangeStepAttempt(
    key: ExchangeStepAttempt.Key,
    finalState: ExchangeStepAttempt.State,
    vararg logEntryMessages: String
  ) {
    val request =
      FinishExchangeStepAttemptRequest.newBuilder()
        .also {
          it.key = key
          it.finalState = finalState
          it.addAllLogEntries(logEntryMessages.map(this::makeLogEntry))
        }
        .build()
    exchangeStepAttemptsClient.finishExchangeStepAttempt(request)
  }

  private fun makeLogEntry(message: String): ExchangeStepAttempt.DebugLog {
    return ExchangeStepAttempt.DebugLog.newBuilder()
      .also {
        it.time = clock.instant().toProtoTime()
        it.message = message
      }
      .build()
  }
}

// TODO(@yunyeng): Import from cross-media-measurement/ProtoUtils.
/** Converts Instant to Timestamp. */
private fun Instant.toProtoTime(): Timestamp =
  Timestamp.newBuilder().setSeconds(epochSecond).setNanos(nano).build()
