package org.wfanet.panelmatch.client.exchangetasks

import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.panelmatch.client.common.ExchangeContext
import org.wfanet.panelmatch.client.storage.PrivateStorageSelector
import org.wfanet.panelmatch.client.storage.SharedStorageSelector
import src.main.kotlin.org.wfanet.panelmatch.client.exchangetasks.SharedStorageTasks

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
