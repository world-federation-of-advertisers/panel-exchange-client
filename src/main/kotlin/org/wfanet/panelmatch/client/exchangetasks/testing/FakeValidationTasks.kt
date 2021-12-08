package org.wfanet.panelmatch.client.exchangetasks.testing

import org.wfanet.panelmatch.client.common.ExchangeContext
import org.wfanet.panelmatch.client.exchangetasks.ValidationTasks

class FakeValidationTasks : ValidationTasks {
  override fun intersectAndValidate(context: ExchangeContext) =
    FakeExchangeTask("intersect-and-validate")
}
