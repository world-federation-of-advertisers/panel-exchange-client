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

import com.google.protobuf.ByteString
import java.io.Serializable
import java.util.BitSet
import kotlin.math.abs
import kotlin.random.Random
import org.apache.beam.sdk.metrics.Metrics
import org.apache.beam.sdk.transforms.DoFn
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.wfanet.panelmatch.common.beam.filter
import org.wfanet.panelmatch.common.beam.groupByKey
import org.wfanet.panelmatch.common.beam.keyBy
import org.wfanet.panelmatch.common.beam.kvOf
import org.wfanet.panelmatch.common.beam.map
import org.wfanet.panelmatch.common.beam.parDo
import org.wfanet.panelmatch.common.beam.values

private const val FAKE_PANELIST_ID: Long = 0
private val FAKE_JOIN_KEY = ByteString.EMPTY

/**
 * Implements a query creation engine in Apache Beam that encrypts a query so that it can later be
 * expanded by another party using oblivious query expansion.
 *
 * @param parameters tuning knobs for the workflow
 * @param privateMembershipCryptor implementation of lower-level oblivious query expansion and
 * result decryption
 */
class CreateQueriesWorkflow(
  private val parameters: Parameters,
  private val privateMembershipCryptor: PrivateMembershipCryptor
) : Serializable {

  /**
   * Tuning knobs for the [BatchCreationWorkflow].
   *
   * @property numShards the number of shards to split the data into
   * @property numBucketsPerShard the number of buckets each shard can have
   * @property totalQueriesPerShard [Int?] pads the number of queries per shard to be this number.
   * If the number of queries is larger than [totalQueriesPerShard], then queries in that shard are
   * culled down to [totalQueriesPerShard]. Null signfies no additional padding/culling should take
   * place. TODO: Implement totalQueriesPerShard
   */
  data class Parameters
  private constructor(
    val numShards: Int,
    val numBucketsPerShard: Int,
    val totalQueriesPerShard: Int?,
    val numQueries: Int,
  ) : Serializable {
    init {
      require(numShards > 0)
      require(numBucketsPerShard > 0)
      totalQueriesPerShard?.let { require(totalQueriesPerShard >= numBucketsPerShard) }
    }
    constructor(
      numShards: Int,
      numBucketsPerShard: Int,
      totalQueriesPerShard: Int?
    ) : this(
      numShards = numShards,
      numBucketsPerShard = numBucketsPerShard,
      totalQueriesPerShard = totalQueriesPerShard,
      numQueries =
        if (totalQueriesPerShard == null) {
          100000
        } else {
          numShards * totalQueriesPerShard
        }
    )
  }

  private data class ShardedData(
    val shardId: ShardId,
    val bucketId: BucketId,
    val panelistKey: PanelistKey,
    val joinKey: JoinKey
  ) : Serializable

  /** Creates [EncryptQueriesResponse] on [data]. */
  fun batchCreateQueries(
    data: PCollection<KV<PanelistKey, JoinKey>>
  ): Pair<PCollection<KV<QueryId, PanelistKey>>, PCollection<PrivateMembershipEncryptResponse>> {
    val shardedData = shardJoinKeys(data)
    val paddedData = addPaddedQueries(shardedData)
    val mappedData = mapToQueryId(paddedData)
    val unencryptedQueries = buildUnencryptedQueryRequest(mappedData)
    val panelistToQueryIdMapping = getPanelistToQueryMapping(mappedData)
    return Pair(panelistToQueryIdMapping, getPrivateMembershipQueries(unencryptedQueries))
  }

  /** Determines shard and bucket for a [JoinKey]. */
  private fun shardJoinKeys(data: PCollection<KV<PanelistKey, JoinKey>>): PCollection<ShardedData> {
    val bucketing = Bucketing(parameters.numShards, parameters.numBucketsPerShard)
    return data.map(name = "Map to ShardId") {
      val (shardId, bucketId) = bucketing.hashAndApply(it.value)
      ShardedData(shardId, bucketId, it.key, it.value)
    }
  }

  /** Wrapper function to add in the necessary number of padded queries */
  private fun addPaddedQueries(data: PCollection<ShardedData>): PCollection<ShardedData> {
    if (parameters.totalQueriesPerShard == null) return data
    return data
      .keyBy { it.shardId }
      .groupByKey("Group into shards")
      .parDo(
        doFn = EqualizeQueriesPerShardFn(requireNotNull(parameters.totalQueriesPerShard)),
        name = "Equalize queries per shard"
      )
  }

  /**
   * Adds or deletes queries from sharded data until it is the desired size. We keep track of which
   * queries are fake so we don't need to decrypt them in the end.
   */
  private class EqualizeQueriesPerShardFn(
    private val totalQueriesPerShard: Int,
  ) : DoFn<KV<ShardId, Iterable<@JvmWildcard ShardedData>>, ShardedData>() {
    /**
     * Metric to track number of discarded Queries. If unacceptably high, the totalQueriesPerShard
     * parameter should be increased.
     */
    private val discardedQueriesMetric =
      Metrics.distribution(CreateQueriesWorkflow::class.java, "discarded-queries")

    @ProcessElement
    fun processElement(context: ProcessContext) {
      val data = context.element()
      var total: Int = 0
      var discardedQueries: Long = 0
      /** Filter out any real queries above the limit */
      data.value.forEach { shardedData ->
        if (total < totalQueriesPerShard) {
          total += 1
          context.output(shardedData)
        } else {
          discardedQueries += 1
        }
      }
      discardedQueriesMetric.update(discardedQueries)
      /** Add queries to get to the limit */
      for (i in total..totalQueriesPerShard - 1) {
        // TODO If we add in query mitigation, the BucketId should be set to the fake bucket
        context.output(
          ShardedData(
            data.key,
            bucketIdOf(0),
            panelistKeyOf(FAKE_PANELIST_ID),
            joinKeyOf(FAKE_JOIN_KEY)
          )
        )
      }
    }
  }

  /**
   * Maps each [PanelistKey] to a unique [QueryId] using an iterator. Works well as long as total
   * collection size is less than ~90% of the mapped [QueryId] space (currently 32 bits). The
   * current iterator uses a BitSet that only supports nonnegative integers which further reduces
   * the mapped space to 16 bits.
   */
  private fun mapToQueryId(data: PCollection<ShardedData>): PCollection<KV<QueryId, ShardedData>> {
    return data.keyBy { 1 }.groupByKey().parDo {
      val queryIds: Iterator<Int> = iterator {
        // TODO - find a better way to do this. It uses too much memory.
        val seen = BitSet()
        while (seen.cardinality() < parameters.numQueries) {
          val id = abs(Random.nextInt())
          if (!seen.get(id)) {
            seen.set(id)
            yield(id)
          }
        }
      }
      it
        .value
        .asSequence()
        .mapIndexed { index, value ->
          require(index < parameters.numQueries) { "Too many queries" }
          kvOf(queryIdOf(queryIds.next()), value)
        }
        .also { yieldAll(it) }
    }
  }

  /** Maps each [PanelistKey] to a unique [QueryId]. We also filter out all the fake queryIds. */
  private fun getPanelistToQueryMapping(
    data: PCollection<KV<QueryId, ShardedData>>
  ): PCollection<KV<QueryId, PanelistKey>> {
    return data
      .filter("Filter out padded queries") {
        it.value.panelistKey.id != FAKE_PANELIST_ID && it.value.joinKey.key != FAKE_JOIN_KEY
      }
      .map("Map to PanelistKey") { kvOf(it.key, it.value.panelistKey) }
  }

  /** Builds [EncryptedQuery] from the encrypted data join keys of [JoinKey]. */
  private fun buildUnencryptedQueryRequest(
    data: PCollection<KV<QueryId, ShardedData>>
  ): PCollection<KV<ShardId, UnencryptedQuery>> {
    return data.map(name = "Map to UnencryptedQuery") {
      kvOf(it.value.shardId, unencryptedQueryOf(it.value.shardId, it.value.bucketId, it.key))
    }
  }

  /** Batch gets the oblivious queries grouped by [ShardId]. */
  private fun getPrivateMembershipQueries(
    data: PCollection<KV<ShardId, UnencryptedQuery>>
  ): PCollection<PrivateMembershipEncryptResponse> {
    return data
      .groupByKey("Group by Shard")
      .map<KV<ShardId, Iterable<UnencryptedQuery>>, KV<ShardId, PrivateMembershipEncryptResponse>>(
        name = "Map to EncryptQueriesResponse"
      ) {
        val encryptQueriesRequest = privateMembershipEncryptRequest {
          unencryptedQueries += it.value
        }
        kvOf(it.key, privateMembershipCryptor.encryptQueries(encryptQueriesRequest))
      }
      .values("Extract Results")
  }
}
