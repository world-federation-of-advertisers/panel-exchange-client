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

import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.measurement.common.throttler.Throttler
import org.wfanet.measurement.common.toLocalDate
import org.wfanet.panelmatch.client.common.ExchangeContext
import org.wfanet.panelmatch.client.storage.PrivateStorageSelector

class PrivateStorageTasksImpl(
  private val privateStorageSelector: PrivateStorageSelector,
  private val inputTaskThrottler: Throttler
) : PrivateStorageTasks {
  override suspend fun getInput(context: ExchangeContext): ExchangeTask {
    val step = context.step
    check(step.stepCase == ExchangeWorkflow.Step.StepCase.INPUT_STEP)
    require(step.inputLabelsMap.isEmpty())
    val blobKey = step.outputLabelsMap.values.single()
    return InputTask(
      storage = privateStorageSelector.getStorageClient(context.exchangeDateKey),
      throttler = inputTaskThrottler,
      blobKey = blobKey,
    )
  }

  override suspend fun copyFromPreviousExchange(context: ExchangeContext): ExchangeTask {
    val step = context.step
    check(step.stepCase == ExchangeWorkflow.Step.StepCase.COPY_FROM_PREVIOUS_EXCHANGE_STEP)

    val previousBlobKey = step.inputLabelsMap.getValue("input")

    val exchangeDateKey = context.exchangeDateKey
    val workflow = context.workflow
    if (exchangeDateKey.date == workflow.firstExchangeDate.toLocalDate()) {
      return InputTask(
        previousBlobKey,
        inputTaskThrottler,
        privateStorageSelector.getStorageClient(exchangeDateKey)
      )
    }

    return CopyFromPreviousExchangeTask(
      privateStorageSelector,
      workflow.repetitionSchedule,
      exchangeDateKey,
      previousBlobKey
    )
  }
}
