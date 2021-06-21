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

package org.wfanet.panelmatch.client.batchlookup.testing

import com.google.protobuf.ByteString
import com.google.protobuf.ListValue
import org.wfanet.panelmatch.client.batchlookup.BucketId
import org.wfanet.panelmatch.client.batchlookup.QueryBundle
import org.wfanet.panelmatch.client.batchlookup.QueryId
import org.wfanet.panelmatch.client.batchlookup.QueryMetadata
import org.wfanet.panelmatch.client.batchlookup.Result
import org.wfanet.panelmatch.client.batchlookup.ShardId

object PlaintextQueryEvaluatorTestHelper : QueryEvaluatorTestHelper {
  override fun decodeResultData(result: Result): ByteString {
    return result.data
  }

  override fun makeQueryBundle(
    shard: ShardId,
    queries: List<Pair<QueryId, BucketId>>
  ): QueryBundle {
    return QueryBundle(
      shard,
      queries.map { QueryMetadata(it.first, ByteString.EMPTY) },
      ListValue.newBuilder()
        .apply {
          for (query in queries) {
            addValuesBuilder().stringValue = query.second.id.toString()
          }
        }
        .build()
        .toByteString()
    )
  }

  override fun makeResult(query: QueryId, rawPayload: ByteString): Result {
    return Result(QueryMetadata(query, ByteString.EMPTY), rawPayload)
  }
}
