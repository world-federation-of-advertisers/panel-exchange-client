package org.wfanet.panelmatch.client.exchangetasks

import org.wfanet.panelmatch.client.common.ExchangeContext

interface MapReduceTasks {
  suspend fun ExchangeContext.getBuildPrivateMembershipQueriesTask(): ExchangeTask

  suspend fun ExchangeContext.getExecutePrivateMembershipQueriesTask(): ExchangeTask

  suspend fun ExchangeContext.getDecryptMembershipResultsTask(): ExchangeTask
}
