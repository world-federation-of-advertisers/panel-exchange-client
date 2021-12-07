package org.wfanet.panelmatch.client.exchangetasks

import com.google.protobuf.ByteString
import org.wfanet.panelmatch.client.common.ExchangeContext
import org.wfanet.panelmatch.client.privatemembership.PrivateMembershipCryptor
import org.wfanet.panelmatch.client.privatemembership.QueryEvaluator
import org.wfanet.panelmatch.client.privatemembership.QueryResultsDecryptor

interface MapReduceTasks {
  suspend fun ExchangeContext.getBuildPrivateMembershipQueriesTask(
    getPrivateMembershipCryptor: (ByteString) -> PrivateMembershipCryptor
  ): ExchangeTask

  suspend fun ExchangeContext.getExecutePrivateMembershipQueriesTask(
    getQueryResultsEvaluator: (ByteString) -> QueryEvaluator
  ): ExchangeTask

  suspend fun ExchangeContext.getDecryptMembershipResultsTask(
    queryResultsDecryptor: QueryResultsDecryptor
  ): ExchangeTask
}
