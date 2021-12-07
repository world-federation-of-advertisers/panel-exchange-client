package org.wfanet.panelmatch.client.exchangetasks

import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.measurement.common.toLocalDate
import org.wfanet.panelmatch.client.common.ExchangeContext

class BasicTasksImpl : BasicTasks {
  override fun ExchangeContext.getIntersectAndValidateStepTask(): ExchangeTask {
    check(step.stepCase == ExchangeWorkflow.Step.StepCase.INTERSECT_AND_VALIDATE_STEP)

    val maxSize = step.intersectAndValidateStep.maxSize
    val maximumNewItemsAllowed = step.intersectAndValidateStep.maximumNewItemsAllowed

    return IntersectValidateTask(
      maxSize = maxSize,
      maximumNewItemsAllowed = maximumNewItemsAllowed,
      isFirstExchange = date == workflow.firstExchangeDate.toLocalDate()
    )
  }
}
