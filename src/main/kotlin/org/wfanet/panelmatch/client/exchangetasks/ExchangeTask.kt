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

package org.wfanet.panelmatch.client.exchangetasks

import com.google.protobuf.ByteString
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttempt
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttemptsGrpcKt.ExchangeStepAttemptsCoroutineStub
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow

/** Interface for ExchangeTask. */
interface ExchangeTask {

  /**
   * Executes given [ExchangeWorkflow.Step] and returns the output.
   *
   * @param step a [ExchangeWorkflow.Step] contains input to be executed.
   * @param attempt current [ExchangeStepAttempt] of the step.
   * @param stub instance of [ExchangeStepAttempts] service.
   * @return executed output.
   */
  suspend fun execute(
    step: ExchangeWorkflow.Step,
    attempt: ExchangeStepAttempt,
    stub: ExchangeStepAttemptsCoroutineStub
  ): Map<String, ByteString>

  /**
   * Executes given input data and returns the output.
   *
   * @param inputs This is a map from the step-specific name to a label for the input.
   * @return executed output.
   */
  suspend fun execute(inputs: Map<String, ByteString>): Map<String, ByteString>
}
