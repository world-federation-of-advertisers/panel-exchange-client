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

package org.wfanet.panelmatch.client.privatemembership.testing

import com.google.protobuf.ListValue
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.wfanet.panelmatch.client.privatemembership.EncryptQueriesResponse
import org.wfanet.panelmatch.client.privatemembership.QueryBundle
import org.wfanet.panelmatch.client.privatemembership.QueryId
import org.wfanet.panelmatch.client.privatemembership.QueryMetadata
import org.wfanet.panelmatch.client.privatemembership.bucketIdOf
import org.wfanet.panelmatch.client.privatemembership.queryIdOf
import org.wfanet.panelmatch.client.privatemembership.shardIdOf
import org.wfanet.panelmatch.common.beam.kvOf
import org.wfanet.panelmatch.common.beam.map
import org.wfanet.panelmatch.common.beam.parDo

object PlaintextPrivateMembershipCryptorHelper : PrivateMembershipCryptorHelper {

  private fun queryBundleOf(shard: Int, queries: List<Pair<Int, Int>>): QueryBundle {
    return PlaintextQueryEvaluatorTestHelper.makeQueryBundle(
      shardIdOf(shard),
      queries.map { queryIdOf(it.first) to bucketIdOf(it.second) }
    )
  }

  private fun decodeQueryBundle(queryBundle: QueryBundle): List<ShardedQuery> {
    val queryBundleList = queryBundle.getQueryMetadataList()
    val bucketValuesList =
      ListValue.parseFrom(queryBundle.payload).getValuesList().map { it.stringValue.toInt() }
    return queryBundleList.zip(bucketValuesList) { a: QueryMetadata, b: Int ->
      ShardedQuery(requireNotNull(queryBundle.shardId).id, a.queryId.id, b)
    }
  }

  override fun decodeEncryptedQuery(
    data: PCollection<EncryptQueriesResponse>
  ): PCollection<KV<QueryId, ShardedQuery>> {
    return data.parDo("Map to ShardedQuery") {
      yieldAll(
        it
          .getCiphertextsList()
          .map { QueryBundle.parseFrom(it) }
          .map { decodeQueryBundle(it) }
          .flatten()
          .asSequence()
          .map { kvOf(it.queryId, ShardedQuery(it.shardId.id, it.queryId.id, it.bucketId.id)) }
      )
    }
  }
}