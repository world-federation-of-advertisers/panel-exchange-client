package org.wfanet.panelmatch.client.batchlookup.testing

import org.wfanet.panelmatch.client.batchlookup.QueryEvaluator

class PlaintextPipelineEndToEndTest : AbstractPipelineEndToEndTest() {
  override val queryEvaluator: QueryEvaluator = PlaintextQueryEvaluator
  override val helper: QueryEvaluatorTestHelper = PlaintextQueryEvaluatorTestHelper
}
