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

package org.wfanet.panelmatch.client.batchlookup

import java.io.Serializable
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.wfanet.panelmatch.common.beam.groupByKey
import org.wfanet.panelmatch.common.beam.kvOf
import org.wfanet.panelmatch.common.beam.map
import org.wfanet.panelmatch.common.beam.values

private fun queryIdGenerator(panelistKey: PanelistKey): QueryId {
  return queryIdOf(panelistKey.id.toInt() * 100)
  // return queryIdOf(randomUUID().getLeastSignificantBits().toInt())
}

/**
 * Implements a batch query creation engine in Apache Beam using oblivious query expansion and result decryption
 *
 * @param parameters tuning knobs for the workflow
 * @param obliviousQueryBuilder implementation of lower-level oblivious query expansion and result decryption
 */
class BatchCreationWorkflow(
  private val parameters: Parameters,
  private val obliviousQueryBuilder: ObliviousQueryBuilder
) : Serializable {

  /**
   * Tuning knobs for the [BatchCreationWorkflow].
   *
   * @property numShards the number of shards to split the data into
   * @property numBucketsPerShard the number of buckets each shard can have
   */
  data class Parameters(val numShards: Int, val numBucketsPerShard: Int) : Serializable {
    init {
      require(numShards > 0)
      require(numBucketsPerShard > 0)
    }
  }

  /** Creates [EncryptQueriesResponse] on [data]. */
  fun batchCreate(
    data: PCollection<KV<PanelistKey, JoinKey>>
  ): PCollection<EncryptQueriesResponse> {
    val mappeddata = mapToQueryId(data)
    val unencryptedQueries = buildUnencryptedQuery(mappeddata)
    return getObliviousQueries(unencryptedQueries)
  }

  /** Maps each [PanelistKey] to a unique [QueryId]. */
  private fun mapToQueryId(
    data: PCollection<KV<PanelistKey, JoinKey>>
  ): PCollection<KV<QueryId, JoinKey>> {
    return data.map {
      val queryId = obliviousQueryBuilder.queryIdGenerator(it.key)
      // TODO output kvOf(it.key, queryId) to MP storage
      kvOf(queryId, it.value)
    }
  }

  /** Builds [EncryptedQuery] from the encrypted data join keys of [JoinKey]. */
  private fun buildUnencryptedQuery(
    data: PCollection<KV<QueryId, JoinKey>>
  ): PCollection<KV<ShardId, UnencryptedQuery>> {
    val bucketing = Bucketing(parameters.numShards, parameters.numBucketsPerShard)
    return data.map {
      val (shardId, bucketId) = bucketing.apply(it.value)
      kvOf(shardId, unencryptedQueryOf(shardId, bucketId, it.key))
    }
  }

  /** Batch gets the oblivious queries grouped by [ShardId]. */
  private fun getObliviousQueries(
    data: PCollection<KV<ShardId, UnencryptedQuery>>
  ): PCollection<EncryptQueriesResponse> {
    return data
      .groupByKey("Group by Shard")
      .map {
        val encryptQueriesRequest =
          EncryptQueriesRequest.newBuilder().addAllUnencryptedQuery(it.value).build()
        kvOf(it.key, obliviousQueryBuilder.encryptQueries(encryptQueriesRequest))
      }
      .values("Extract Results")
  }
}
