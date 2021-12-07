package org.wfanet.panelmatch.client.exchangetasks

import org.wfanet.panelmatch.client.common.ExchangeContext

interface BasicTasks {
  fun ExchangeContext.getIntersectAndValidateStepTask(): ExchangeTask
}
