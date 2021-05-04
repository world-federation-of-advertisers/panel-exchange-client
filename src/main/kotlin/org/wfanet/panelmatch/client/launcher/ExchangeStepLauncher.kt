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

import org.wfanet.measurement.api.v2alpha.DataProvider
import org.wfanet.measurement.api.v2alpha.ExchangeStep
import org.wfanet.measurement.api.v2alpha.ExchangeStepsGrpcKt.ExchangeStepsCoroutineStub
import org.wfanet.measurement.api.v2alpha.FindReadyExchangeStepRequest
import org.wfanet.measurement.api.v2alpha.FindReadyExchangeStepResponse

/** Finds an [ExchangeStep], validates it, and starts executing the work. */
class ExchangeStepLauncher(
  private val exchangeStepsClient: ExchangeStepsCoroutineStub,
  private val dataProviderId: String
) {

  /**
   * Finds a single Exchange Step and starts executing.
   *
   * @throws IllegalArgumentException if Exchange Step is not valid.
   */
  suspend fun findAndRunExchangeStep() {
    val exchangeStep = findExchangeStep() ?: return
    validateExchangeStep(exchangeStep)
    runExchangeStep(exchangeStep)
  }

  /**
   * Finds a single Exchange Step from ExchangeSteps service.
   *
   * @return single [ExchangeStep].
   */
  suspend fun findExchangeStep(): ExchangeStep? {
    val key = DataProvider.Key.newBuilder().setDataProviderId(dataProviderId).build()
    val request: FindReadyExchangeStepRequest =
      FindReadyExchangeStepRequest.newBuilder().setDataProvider(key).build()
    // Call /ExchangeSteps.findReadyExchangeStep to a find work to do.
    val response: FindReadyExchangeStepResponse = exchangeStepsClient.findReadyExchangeStep(request)
    if (response.hasExchangeStep()) {
      return response.exchangeStep
    }
    return null
  }

  /**
   * Starts executing the given Exchange Step.
   *
   * @param exchangeStep [ExchangeStep].
   */
  fun runExchangeStep(exchangeStep: ExchangeStep) {
    // TODO(@yunyeng): Start JobStarter with the exchangeStep.
  }

  /**
   * Validates the given Exchange Step.
   *
   * @param exchangeStep [ExchangeStep].
   * @throws IllegalArgumentException if the Exchange Step is not valid.
   */
  fun validateExchangeStep(exchangeStep: ExchangeStep) {
    // Validate that this exchange step is legal, otherwise throw an error.
    // TODO(@yunyeng): Add validation logic.
  }
}
