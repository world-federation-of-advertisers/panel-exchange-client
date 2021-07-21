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

import com.google.protobuf.ByteString
import java.io.Serializable
import org.apache.beam.sdk.transforms.GroupByKey
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.wfanet.panelmatch.common.beam.combinePerKey
import org.wfanet.panelmatch.common.beam.join
import org.wfanet.panelmatch.common.beam.keyBy
import org.wfanet.panelmatch.common.beam.kvOf
import org.wfanet.panelmatch.common.beam.map
import org.wfanet.panelmatch.common.beam.parDo
import org.wfanet.panelmatch.common.beam.values

/** Label for input database keys. */
data class DatabaseKey(val id: Long)

/** Label for original database contents. */
data class Plaintext(val data: ByteString)

/**
 * Implements a batch query engine in Apache Beam using homomorphic encryption.
 *
 * @param parameters tuning knobs for the workflow
 * @param queryEvaluator implementation of lower-level homomorphic operations
 */
class BatchLookupWorkflow(
  private val parameters: Parameters,
  private val queryEvaluator: QueryEvaluator
) : Serializable {

  /**
   * Tuning knobs for the [BatchLookupWorkflow].
   *
   * The [subshardSizeBytes] property should be set to the largest value possible such that the
   * pipeline does not experience out-of-memory errors, or is unable to exploit all available
   * parallelism, or is blocked on I/O. We recommend tuning this parameter experimentally -- it
   * should fall somewhere between 10MiB and 1GiB, most likely.
   *
   * @property numShards the number of shards to split the database into
   * @property numBucketsPerShard the number of buckets each shard can have
   * @property subshardSizeBytes the maximum size of a [DatabaseShard] before it is split up
   */
  data class Parameters(
    val numShards: Int,
    val numBucketsPerShard: Int,
    val subshardSizeBytes: Int
  ) : Serializable {
    init {
      require(numShards > 0)
      require(numBucketsPerShard > 0)
      require(subshardSizeBytes > 0)
    }
  }

  /** Executes [queryBundles] on [database]. */
  fun batchLookup(
    database: PCollection<KV<DatabaseKey, Plaintext>>,
    queryBundles: PCollection<QueryBundle>
  ): PCollection<Result> {
    val shardedDatabase: PCollection<KV<ShardId, DatabaseShard>> = shardDatabase(database)

    val queriesByShard = queryBundles.keyBy("Key QueryBundles by Shard") { it.shardId }

    val uncombinedResults: PCollection<KV<KV<ShardId, QueryId>, Result>> =
      querySubshards(shardedDatabase, queriesByShard)

    return uncombinedResults
      .combinePerKey("Combine Subshard Results") { queryEvaluator.combineResults(it.asSequence()) }
      .values("Extract Results")
  }

  /** Joins the inputs to execute the queries on the appropriate shards. */
  private fun querySubshards(
    shardedDatabase: PCollection<KV<ShardId, DatabaseShard>>,
    queriesByShard: PCollection<KV<ShardId, QueryBundle>>
  ): PCollection<KV<KV<ShardId, QueryId>, Result>> {
    return shardedDatabase.join(queriesByShard, name = "Join Database and queryMetadata") {
      key,
      shards,
      queries ->
      val queriesList = queries.toList()

      val nonEmptyShards: Iterable<DatabaseShard> =
        if (shards.iterator().hasNext()) {
          shards
        } else {
          listOf(DatabaseShard(key, emptyList()))
        }

      for (shard in nonEmptyShards) {
        val results = queryEvaluator.executeQueries(listOf(shard), queriesList)
        for (result in results) {
          yield(kvOf(kvOf(key, result.queryMetadata.queryId), result))
        }
      }
    }
  }

  /** Splits the database into [DatabaseShard]s of appropriate size. */
  private fun shardDatabase(
    database: PCollection<KV<DatabaseKey, Plaintext>>
  ): PCollection<KV<ShardId, DatabaseShard>> {
    val bucketing = Bucketing(parameters.numShards, parameters.numBucketsPerShard)
    return database
      .map {
        val (shardId, bucketId) = bucketing.apply(it.key.id)
        kvOf(shardId, Bucket(bucketId, it.value.data))
      }
      .apply("Group into Batches", GroupByKey.create())
      .parDo {
        // While this might look like exactly what GroupIntoBatches does, it's not. GroupIntoBatches
        // does not guarantee a strict size limit. We, on the other hand, need a strict size limit
        // here because we're trying to build as big batches as possible without OOMing.
        var size = 0
        var buffer = mutableListOf<Bucket>()
        val shardId: ShardId = requireNotNull(it.key)
        val buckets: Iterable<Bucket> = requireNotNull(it.value)
        for (bucket in buckets) {
          val bucketSize = bucket.data.size()
          if (size + bucketSize > parameters.subshardSizeBytes) {
            yield(kvOf(shardId, DatabaseShard(shardId, buffer)))
            buffer = mutableListOf()
            size = 0
          }
          size += bucketSize
          buffer.add(bucket)
        }
        if (buffer.isNotEmpty()) {
          yield(kvOf(shardId, DatabaseShard(shardId, buffer)))
        }
      }
  }
}