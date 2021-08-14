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

package org.wfanet.panelmatch.client.privatemembership

import java.io.Serializable
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.wfanet.panelmatch.common.beam.groupByKey
import org.wfanet.panelmatch.common.beam.keyBy
import org.wfanet.panelmatch.common.beam.kvOf
import org.wfanet.panelmatch.common.beam.map
import org.wfanet.panelmatch.common.beam.parDo
import org.wfanet.panelmatch.common.beam.values

/**
 * Implements a query creation engine in Apache Beam that encrypts a query so that it can later be
 * expanded by another party using oblivious query expansion.
 *
 * @param parameters tuning knobs for the workflow
 * @param obliviousQueryBuilder implementation of lower-level oblivious query expansion and result
 * decryption
 */
class CreateQueriesWorkflow(
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
  fun batchCreateQueries(
    data: PCollection<KV<PanelistKey, JoinKey>>
  ): Pair<PCollection<KV<PanelistKey, QueryId>>, PCollection<EncryptQueriesResponse>> {
    val mappedData = mapToQueryId(data)
    val panelistToQueryIdMapping = getPanelistToQueryMapping(mappedData)
    val unencryptedQueries = buildUnencryptedQuery(mappedData)
    return Pair(panelistToQueryIdMapping, getObliviousQueries(unencryptedQueries))
  }

  /** Maps each [PanelistKey] to a unique [QueryId]. */
  private fun mapToQueryId(
    data: PCollection<KV<PanelistKey, JoinKey>>
  ): PCollection<KV<KV<PanelistKey, QueryId>, JoinKey>> {
    return data.keyBy { 1 }.groupByKey().parDo {
      it
        .value
        .asSequence()
        .shuffled()
        .mapIndexed { index, kv ->
          kvOf(kvOf(requireNotNull(kv.key), queryIdOf(index)), requireNotNull(kv.value))
        }
        .asSequence()
        .forEach { yield(it) }
    }
  }

  /** Maps each [PanelistKey] to a unique [QueryId]. */
  private fun getPanelistToQueryMapping(
    data: PCollection<KV<KV<PanelistKey, QueryId>, JoinKey>>
  ): PCollection<KV<PanelistKey, QueryId>> {
    return data.map("Map of PanelistKey to QueryId") { kvOf(it.key.key, it.key.value) }
  }

  /** Builds [EncryptedQuery] from the encrypted data join keys of [JoinKey]. */
  private fun buildUnencryptedQuery(
    data: PCollection<KV<KV<PanelistKey, QueryId>, JoinKey>>
  ): PCollection<KV<ShardId, UnencryptedQuery>> {
    val bucketing = Bucketing(parameters.numShards, parameters.numBucketsPerShard)
    return data.map(name = "Map to UnencryptedQuery") {
      val (shardId, bucketId) = bucketing.hashAndApply(it.value)
      kvOf(shardId, unencryptedQueryOf(shardId, bucketId, it.key.value))
    }
  }

  /** Batch gets the oblivious queries grouped by [ShardId]. */
  private fun getObliviousQueries(
    data: PCollection<KV<ShardId, UnencryptedQuery>>
  ): PCollection<EncryptQueriesResponse> {
    return data
      .groupByKey("Group by Shard")
      .map(name = "Map to EncryptQueriesResponse") {
        val encryptQueriesRequest =
          EncryptQueriesRequest.newBuilder().addAllUnencryptedQuery(it.value).build()
        kvOf(it.key, obliviousQueryBuilder.encryptQueries(encryptQueriesRequest))
      }
      .values("Extract Results")
  }
}
