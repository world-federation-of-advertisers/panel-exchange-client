package org.wfanet.panelmatch.client.exchangetasks.testing

import org.wfanet.panelmatch.client.common.ExchangeContext
import org.wfanet.panelmatch.client.exchangetasks.MapReduceTasks

class FakeMapReduceTasks : MapReduceTasks {
  override suspend fun buildPrivateMembershipQueries(context: ExchangeContext) =
    FakeExchangeTask("build-private-membership-queries")
  override suspend fun executePrivateMembershipQueries(context: ExchangeContext) =
    FakeExchangeTask("execute-private-membership-queries")
  override suspend fun decryptMembershipResults(context: ExchangeContext) =
    FakeExchangeTask("decrypt-membership-results")
}
