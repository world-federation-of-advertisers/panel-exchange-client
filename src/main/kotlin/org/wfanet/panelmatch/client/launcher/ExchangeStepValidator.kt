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

import com.google.protobuf.InvalidProtocolBufferException
import java.time.Clock
import org.wfanet.measurement.api.v2alpha.ExchangeStep
import org.wfanet.measurement.api.v2alpha.ExchangeStepKey
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.measurement.common.toLocalDate
import org.wfanet.panelmatch.client.launcher.InvalidExchangeStepException.FailureType.PERMANENT
import org.wfanet.panelmatch.client.launcher.InvalidExchangeStepException.FailureType.TRANSIENT
import org.wfanet.panelmatch.common.secrets.SecretMap

/** Indicates that an [ExchangeStep] is not valid to execute. */
class InvalidExchangeStepException(val type: FailureType, message: String) : Exception(message) {
  enum class FailureType {
    PERMANENT,
    TRANSIENT
  }
}

/** Determines whether an ExchangeStep is valid and can be safely executed. */
class ExchangeStepValidator(
  private val party: ExchangeWorkflow.Party,
  private val validExchangeWorkflows: SecretMap,
  private val clock: Clock
) {
  /** Throws [InvalidExchangeStepException] if [exchangeStep] is invalid. */
  suspend fun validate(exchangeStep: ExchangeStep) {
    val serializedExchangeWorkflow = exchangeStep.serializedExchangeWorkflow
    val recurringExchangeId =
      requireNotNull(ExchangeStepKey.fromName(exchangeStep.name)).recurringExchangeId
    if (validExchangeWorkflows.get(recurringExchangeId) != serializedExchangeWorkflow) {
      throw InvalidExchangeStepException(PERMANENT, "Serialized ExchangeWorkflow unrecognized")
    }

    @Suppress("BlockingMethodInNonBlockingContext") // Proto parsing is lightweight
    val workflow =
      try {
        ExchangeWorkflow.parseFrom(serializedExchangeWorkflow)
      } catch (e: InvalidProtocolBufferException) {
        throw InvalidExchangeStepException(PERMANENT, "Invalid ExchangeWorkflow")
      }

    val stepIndex = exchangeStep.stepIndex
    if (stepIndex !in 0 until workflow.stepsCount) {
      throw InvalidExchangeStepException(PERMANENT, "Invalid step_index: $stepIndex")
    }
    val step = workflow.getSteps(stepIndex)
    if (step.party != party) {
      throw InvalidExchangeStepException(
        PERMANENT,
        "Party for step '${step.stepId}' was not ${party.name}"
      )
    }

    val firstExchangeDate = workflow.firstExchangeDate.toLocalDate()
    val exchangeDate = exchangeStep.exchangeDate.toLocalDate()
    if (exchangeDate < firstExchangeDate) {
      throw InvalidExchangeStepException(
        PERMANENT,
        "exchange_date is before ExchangeWorkflow.first_exchange_date"
      )
    }

    val exchangeTime = exchangeDate.atStartOfDay(clock.zone).toInstant()
    if (exchangeTime > clock.instant()) {
      throw InvalidExchangeStepException(TRANSIENT, "exchange_date is in the future")
    }
  }
}
