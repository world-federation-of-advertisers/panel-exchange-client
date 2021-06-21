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

data class Parameters(val numShards: Int, val numBucketsPerShard: Int, val subshardSizeBytes: Int) :
  Serializable {
  init {
    require(numShards > 0)
    require(numBucketsPerShard > 0)
    require(subshardSizeBytes > 0)
  }
}

class BatchLookupWorkflow(
  private val parameters: Parameters,
  private val batchLookupClient: BatchLookupClient
) : Serializable {

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
      .combinePerKey("Combine Subshard Results") {
        batchLookupClient.combineResults(it.asSequence())
      }
      .values("Extract Results")
  }

  fun querySubshards(
    shardedDatabase: PCollection<KV<ShardId, DatabaseShard>>,
    queriesByShard: PCollection<KV<ShardId, QueryBundle>>
  ): PCollection<KV<KV<ShardId, QueryId>, Result>> {
    return shardedDatabase.join(queriesByShard, name = "Join Database and queries") {
      key,
      shards,
      queries ->
      val queriesList = queries.toList()
      for (shard in shards) {
        val results = batchLookupClient.executeQueries(listOf(shard), queriesList)
        for (result in results) {
          yield(kvOf(kvOf(key, result.queryId), result))
        }
      }
    }
  }

  fun shardDatabase(
    database: PCollection<KV<DatabaseKey, Plaintext>>
  ): PCollection<KV<ShardId, DatabaseShard>> {
    return database
      .map { kvOf(shard(it.key.id), Bucket(bucket(it.key.id), it.value.data)) }
      // TODO: in Apache Beam 2.31.0, use GroupIntoBatches.
      .apply("Group into Batches", GroupByKey.create())
      .parDo {
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

  private fun shard(value: Long): ShardId {
    return ShardId((value % parameters.numShards).toInt())
  }

  private fun bucket(value: Long): BucketId {
    return BucketId(((value / parameters.numShards) % parameters.numBucketsPerShard).toInt())
  }
}
