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
import org.wfanet.panelmatch.client.common.ExchangeContext
import org.wfanet.panelmatch.client.storage.PrivateStorageSelector
import org.wfanet.panelmatch.client.storage.SharedStorageSelector

class SharedStorageTasksImpl(
  private val privateStorageSelector: PrivateStorageSelector,
  private val sharedStorageSelector: SharedStorageSelector
) : SharedStorageTasks {
  override suspend fun ExchangeContext.buildCopyFromSharedStorageTask(): ExchangeTask {
    check(step.stepCase == ExchangeWorkflow.Step.StepCase.COPY_FROM_SHARED_STORAGE_STEP)
    val source = sharedStorageSelector.getSharedStorage(workflow.exchangeIdentifiers.storage, this)
    val destination = privateStorageSelector.getStorageClient(exchangeDateKey)
    return CopyFromSharedStorageTask(
      source,
      destination,
      step.copyFromSharedStorageStep.copyOptions,
      step.inputLabelsMap.values.single(),
      step.outputLabelsMap.values.single()
    )
  }

  override suspend fun ExchangeContext.buildCopyToSharedStorageTask(): ExchangeTask {
    check(step.stepCase == ExchangeWorkflow.Step.StepCase.COPY_TO_SHARED_STORAGE_STEP)
    val source = privateStorageSelector.getStorageClient(exchangeDateKey)
    val destination =
      sharedStorageSelector.getSharedStorage(workflow.exchangeIdentifiers.storage, this)
    return CopyToSharedStorageTask(
      source,
      destination,
      step.copyToSharedStorageStep.copyOptions,
      step.inputLabelsMap.values.single(),
      step.outputLabelsMap.values.single()
    )
  }
}
