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
import java.io.Serializable
import org.wfanet.panelmatch.client.batchlookup.BucketId
import org.wfanet.panelmatch.client.batchlookup.QueryBundle
import org.wfanet.panelmatch.client.batchlookup.QueryId
import org.wfanet.panelmatch.client.batchlookup.Result
import org.wfanet.panelmatch.client.batchlookup.ShardId

// TODO: factor out QueryEvaluatorTestHelper. Much of its functionality is already moved to the
// ObliviousQueryBuilder
interface QueryEvaluatorTestHelper : Serializable {
  data class DecodedResult(val queryId: Long, val data: ByteString) : Serializable {
    override fun toString(): String {
      return "DecodedResult(query=$queryId, data=${data.toStringUtf8()}"
    }
  }

  fun decodeResult(result: Result): DecodedResult {
    return DecodedResult(result.queryMetadata.queryId.id, decodeResultData(result))
  }

  fun decodeResultData(result: Result): ByteString

  fun makeQueryBundle(shard: ShardId, queries: List<Pair<QueryId, BucketId>>): QueryBundle

  fun makeResult(query: QueryId, rawPayload: ByteString): Result
}
