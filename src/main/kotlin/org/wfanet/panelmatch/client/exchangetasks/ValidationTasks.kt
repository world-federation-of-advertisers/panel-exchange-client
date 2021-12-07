package org.wfanet.panelmatch.client.exchangetasks

import org.wfanet.panelmatch.client.common.ExchangeContext

interface ValidationTasks {
  fun ExchangeContext.getIntersectAndValidateStepTask(): ExchangeTask
}
