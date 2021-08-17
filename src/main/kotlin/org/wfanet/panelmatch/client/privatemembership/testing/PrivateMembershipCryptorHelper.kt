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

import java.io.Serializable
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.wfanet.panelmatch.client.privatemembership.EncryptQueriesResponse
import org.wfanet.panelmatch.client.privatemembership.PanelistKey
import org.wfanet.panelmatch.client.privatemembership.QueryId
import org.wfanet.panelmatch.client.privatemembership.bucketIdOf
import org.wfanet.panelmatch.client.privatemembership.panelistKeyOf
import org.wfanet.panelmatch.client.privatemembership.queryIdOf
import org.wfanet.panelmatch.client.privatemembership.shardIdOf
import org.wfanet.panelmatch.common.beam.join
import org.wfanet.panelmatch.common.beam.kvOf

/** Used for testing CreateQueriesWorkflow (eg reversing some of the operations) */
interface PrivateMembershipCryptorHelper : Serializable {

  fun decodeEncryptedQuery(
    data: PCollection<EncryptQueriesResponse>
  ): PCollection<KV<QueryId, ShardedQuery>>

  fun getPanelistQueries(
    decryptedQueries: PCollection<KV<QueryId, ShardedQuery>>,
    panelistKeyQueryId: PCollection<KV<QueryId, PanelistKey>>
  ): PCollection<KV<QueryId, PanelistQuery>> {
    return panelistKeyQueryId.join(decryptedQueries) {
      key: QueryId,
      lefts: Iterable<PanelistKey>,
      rights: Iterable<ShardedQuery> ->
      yield(
        kvOf(
          key,
          PanelistQuery(rights.first().shardId.id, lefts.first().id, rights.first().bucketId.id)
        )
      )
    }
  }
}

data class ShardedQuery(private val shard: Int, private val query: Int, private val bucket: Int) :
  Serializable {
  val shardId = shardIdOf(shard)
  val queryId = queryIdOf(query)
  val bucketId = bucketIdOf(bucket)
}

data class PanelistQuery(
  private val shard: Int,
  private val panelist: Long,
  private val bucket: Int
) : Serializable {
  val shardId = shardIdOf(shard)
  val panelistKey = panelistKeyOf(panelist)
  val bucketId = bucketIdOf(bucket)
}
