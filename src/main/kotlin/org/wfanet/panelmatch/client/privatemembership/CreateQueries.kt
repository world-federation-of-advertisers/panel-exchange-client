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
import org.apache.beam.sdk.metrics.Metrics
import org.apache.beam.sdk.transforms.DoFn
import org.apache.beam.sdk.transforms.PTransform
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.apache.beam.sdk.values.PCollectionTuple
import org.apache.beam.sdk.values.PCollectionView
import org.apache.beam.sdk.values.TupleTag
import org.wfanet.panelmatch.client.common.bucketIdOf
import org.wfanet.panelmatch.client.common.joinKeyOf
import org.wfanet.panelmatch.client.common.panelistKeyOf
import org.wfanet.panelmatch.client.common.queryIdOf
import org.wfanet.panelmatch.client.common.unencryptedQueryOf
import org.wfanet.panelmatch.common.beam.filter
import org.wfanet.panelmatch.common.beam.groupByKey
import org.wfanet.panelmatch.common.beam.kvOf
import org.wfanet.panelmatch.common.beam.map
import org.wfanet.panelmatch.common.beam.mapWithSideInput
import org.wfanet.panelmatch.common.beam.parDo
import org.wfanet.panelmatch.common.crypto.AsymmetricKeys

private const val FAKE_PANELIST_ID: Long = 0
private val FAKE_JOIN_KEY = ByteString.EMPTY

/**
 * Implements a query creation engine in Apache Beam that encrypts a query so that it can later be
 * expanded by another party using oblivious query expansion.
 */
fun createQueries(
  panelistKeyAndJoinKey: PCollection<PanelistKeyAndJoinKey>,
  privateMembershipKeys: PCollectionView<AsymmetricKeys>,
  parameters: CreateQueriesParameters,
  privateMembershipCryptor: PrivateMembershipCryptor
): CreateQueriesOutputs {
  val tuple =
    panelistKeyAndJoinKey.apply(
      "Create Queries",
      CreateQueries(privateMembershipKeys, parameters, privateMembershipCryptor)
    )
  return CreateQueriesOutputs(
    queryIdMap = tuple[CreateQueries.queryIdAndJoinKeysTag],
    encryptedQueryBundles = tuple[CreateQueries.encryptedQueryBundlesTag]
  )
}

data class CreateQueriesParameters(
  val workflowParameters: WorkflowParameters,
  val padQueries: Boolean
)

data class CreateQueriesOutputs(
  val queryIdMap: PCollection<QueryIdAndPanelistKey>,
  val encryptedQueryBundles: PCollection<EncryptedQueryBundle>
)

private class CreateQueries(
  private val privateMembershipKeys: PCollectionView<AsymmetricKeys>,
  private val parameters: CreateQueriesParameters,
  private val privateMembershipCryptor: PrivateMembershipCryptor
) : PTransform<PCollection<PanelistKeyAndJoinKey>, PCollectionTuple>() {

  private val workflowParameters: WorkflowParameters
    get() = parameters.workflowParameters

  override fun expand(input: PCollection<PanelistKeyAndJoinKey>): PCollectionTuple {
    val queriesByShard = shardJoinKeys(input)
    val paddedQueriesByShard = addPaddedQueries(queriesByShard)
    val queriesWithQueryIds = mapToQueryId(paddedQueriesByShard)
    val unencryptedQueries = buildUnencryptedQueryRequest(queriesWithQueryIds)
    val panelistToQueryIdMapping = convertToQueryIdAndPanelistKeys(queriesWithQueryIds)
    val encryptedQueryBundles = encryptQueries(unencryptedQueries, privateMembershipKeys)
    return PCollectionTuple.of(queryIdAndJoinKeysTag, panelistToQueryIdMapping)
      .and(encryptedQueryBundlesTag, encryptedQueryBundles)
  }

  companion object {
    val queryIdAndJoinKeysTag = TupleTag<QueryIdAndPanelistKey>()
    val encryptedQueryBundlesTag = TupleTag<EncryptedQueryBundle>()
  }

  /** Determines shard and bucket for a [JoinKey]. */
  private fun shardJoinKeys(
    queries: PCollection<PanelistKeyAndJoinKey>
  ): PCollection<KV<ShardId, Iterable<@JvmWildcard BucketQuery>>> {
    val bucketing =
      Bucketing(
        numShards = workflowParameters.numShards,
        numBucketsPerShard = workflowParameters.numBucketsPerShard
      )

    return queries
      .map("Map to ShardId") {
        val (shardId, bucketId) = bucketing.hashAndApply(it.joinKey)
        kvOf(shardId, BucketQuery(shardId, bucketId, it.panelistKey, it.joinKey))
      }
      .groupByKey("Group by Shard")
  }

  /** Wrapper function to add in the necessary number of padded queries */
  private fun addPaddedQueries(
    queries: PCollection<KV<ShardId, Iterable<@JvmWildcard BucketQuery>>>
  ): PCollection<KV<ShardId, Iterable<@JvmWildcard BucketQuery>>> {
    if (!parameters.padQueries) return queries
    val totalQueriesPerShard = workflowParameters.maxQueriesPerShard
    return queries.parDo(
      EqualizeQueriesPerShardFn(totalQueriesPerShard),
      name = "Equalize Queries per Shard"
    )
  }

  /**
   * Maps each [PanelistKey] to a unique [QueryId].
   *
   * The range [0, Int.MAX_VALUE) is partitioned into a sub-range per shard and then the queries in
   * each shard are randomly assigned distinct ids from the sub-range.
   *
   * For example, if there are 10 shards, then queries from the first shard are assigned ids from
   * [0, x), queries from the second shard from [x, 2 * x), where x = Int.MAX_VALUE / 10.
   *
   * This is efficient because it can process each shard in parallel, and under the hood, a Bloom
   * filter is used as an efficient way to filter out possibly-seen query ids.
   */
  private fun mapToQueryId(
    queries: PCollection<KV<ShardId, Iterable<@JvmWildcard BucketQuery>>>
  ): PCollection<KV<QueryId, BucketQuery>> {
    val numShards = workflowParameters.numShards
    val queryIdUpperBound = Int.MAX_VALUE / numShards
    return queries.parDo("Add QueryIds") { kv ->
      val offset = kv.key.id * queryIdUpperBound
      val queryIds = iterateUniqueQueryIds(queryIdUpperBound)
      for (query in kv.value) {
        val queryId = queryIds.next() + offset
        yield(kvOf(queryIdOf(queryId), query))
      }
    }
  }

  /** Filter out fake queries and return [QueryIdAndPanelistKey]s. */
  private fun convertToQueryIdAndPanelistKeys(
    data: PCollection<KV<QueryId, BucketQuery>>
  ): PCollection<QueryIdAndPanelistKey> {
    return data
      .filter("Filter out padded queries") {
        it.value.panelistKey.id != FAKE_PANELIST_ID && it.value.joinKey.key != FAKE_JOIN_KEY
      }
      .map("Map to QueryIdAndPanelistKey") {
        queryIdAndPanelistKey {
          queryId = it.key
          panelistKey = it.value.panelistKey
        }
      }
  }

  /** Builds [UnencryptedQuery] from the encrypted data join keys of [JoinKey]. */
  private fun buildUnencryptedQueryRequest(
    data: PCollection<KV<QueryId, BucketQuery>>
  ): PCollection<KV<ShardId, UnencryptedQuery>> {
    return data.map(name = "Map to UnencryptedQuery") { kv ->
      kvOf(kv.value.shardId, unencryptedQueryOf(kv.value.shardId, kv.value.bucketId, kv.key))
    }
  }

  /** Batch gets the oblivious queries grouped by [ShardId]. */
  private fun encryptQueries(
    unencryptedQueries: PCollection<KV<ShardId, UnencryptedQuery>>,
    privateMembershipKeys: PCollectionView<AsymmetricKeys>
  ): PCollection<EncryptedQueryBundle> {
    // Local reference because DecryptQueryResultsWorkflow is not serializable.
    val privateMembershipCryptor = privateMembershipCryptor
    return unencryptedQueries.groupByKey("Group by Shard").mapWithSideInput(
        privateMembershipKeys,
        "Map to EncryptQueriesResponse"
      ) { kv, keys ->
      encryptedQueryBundle {
        shardId = kv.key
        queryIds += kv.value.map { it.queryId }
        serializedEncryptedQueries = privateMembershipCryptor.encryptQueries(kv.value, keys)
      }
    }
  }
}

private data class BucketQuery(
  val shardId: ShardId,
  val bucketId: BucketId,
  val panelistKey: PanelistKey,
  val joinKey: JoinKey
) : Serializable

/**
 * Adds or deletes queries from sharded data until it is the desired size. We keep track of which
 * queries are fake so we don't need to decrypt them in the end.
 */
private class EqualizeQueriesPerShardFn(private val totalQueriesPerShard: Int) :
  DoFn<
    KV<ShardId, Iterable<@JvmWildcard BucketQuery>>,
    KV<ShardId, Iterable<@JvmWildcard BucketQuery>>>() {
  private val metricNamespace = "CreateQueries"

  /**
   * Number of discarded Queries. If unacceptably high, the totalQueriesPerShard parameter should be
   * increased.
   */
  private val discardedQueriesDistribution =
    Metrics.distribution(metricNamespace, "discarded-queries-per-shard")

  /** Number of padding queries added to each shard. */
  private val paddingQueriesDistribution =
    Metrics.distribution(metricNamespace, "padding-queries-per-shard")

  @ProcessElement
  fun processElement(context: ProcessContext) {
    val kv = context.element()
    val allQueries = kv.value.toList()

    val queryCountDelta = allQueries.size - totalQueriesPerShard
    discardedQueriesDistribution.update(maxOf(0L, queryCountDelta.toLong()))

    if (queryCountDelta >= 0) {
      context.output(kvOf(kv.key, allQueries.take(totalQueriesPerShard)))
      return
    }

    paddingQueriesDistribution.update(-queryCountDelta.toLong())
    val paddingQueries =
      List(-queryCountDelta) {
        // TODO If we add in query mitigation, the BucketId should be set to the fake bucket
        BucketQuery(
          kv.key,
          bucketIdOf(0),
          panelistKeyOf(FAKE_PANELIST_ID),
          joinKeyOf(FAKE_JOIN_KEY)
        )
      }

    context.output(kvOf(kv.key, allQueries + paddingQueries))
  }
}
