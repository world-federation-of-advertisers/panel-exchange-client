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

class ExchangeStepLauncher(private val exchangeStepsClient: ExchangeStepsCoroutineStub) {

  /**
   * Finds a single ExchangeTask and starts executing.
   *
   * @param dataProviderId Data Provider id.
   * @throws NoSuchElementException If no ExchangeTask exists.
   * @throws IllegalArgumentException if ExchangeTask is not valid.
   */
  suspend fun findAndRunExchangeTask(dataProviderId: String) {
    val exchangeStep = validateExchangeTask(findExchangeTask(dataProviderId))
    if (exchangeStep != null) {
      runExchangeTask(exchangeStep)
    }
  }

  /**
   * Finds a single ExchangeTask from ExchangeSteps service.
   *
   * @param dataProviderId Data Provider id.
   * @return single [ExchangeStep].
   * @throws NoSuchElementException If no ExchangeTask exists.
   */
  suspend fun findExchangeTask(dataProviderId: String): ExchangeStep? {
    val key = DataProvider.Key.newBuilder().setDataProviderId(dataProviderId).build()
    val request: FindReadyExchangeStepRequest =
      FindReadyExchangeStepRequest.newBuilder().setDataProvider(key).build()
    // Call /ExchangeSteps.findReadyExchangeStep to a find work to do.
    val response: FindReadyExchangeStepResponse = exchangeStepsClient.findReadyExchangeStep(request)
    if (response.hasExchangeStep()) {
      return response.exchangeStep
    }
    throw NoSuchElementException("There is not any Exchange Step in Ready state.")
  }

  /**
   * Starts executing the given ExchangeTask.
   *
   * @param exchangeStep [ExchangeStep].
   */
  fun runExchangeTask(exchangeStep: ExchangeStep) {
    // TODO(@yunyeng): Start JobStarter with the exchangeStep.
  }

  /**
   * Validates the given ExchangeTask.
   *
   * @param exchangeStep [ExchangeStep].
   * @return validated [ExchangeStep] or null.
   * @throws IllegalArgumentException if ExchangeTask is not valid.
   */
  fun validateExchangeTask(exchangeStep: ExchangeStep?): ExchangeStep? {
    // Validate that this exchange step is legal.
    // TODO(@yunyeng): Add validation logic.
    return null
  }
}
