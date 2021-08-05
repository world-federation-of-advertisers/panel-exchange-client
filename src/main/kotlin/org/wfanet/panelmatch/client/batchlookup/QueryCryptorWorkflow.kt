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
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.wfanet.panelmatch.common.beam.combinePerKey
import org.wfanet.panelmatch.common.beam.groupByKey
import org.wfanet.panelmatch.common.beam.join
import org.wfanet.panelmatch.common.beam.keyBy
import org.wfanet.panelmatch.common.beam.kvOf
import org.wfanet.panelmatch.common.beam.map
import org.wfanet.panelmatch.common.beam.parDo
import org.wfanet.panelmatch.common.beam.values
import java.util.UUID

class QueryCryptorWorkflow(
  private val parameters: Parameters,
  private val queryCryptor: QueryCryptor
) : Serializable {

  /**
   * Tuning parameters that both parties agree to for panel exchange for the [QueryCryptorWorkflow].
   *
   * @property numShards the number of shards to split the database into
   * @property numBucketsPerShard the number of buckets each shard can have
   */
  data class Parameters(
    val numShards: Int,
    val numBucketsPerShard: Int
  ) : Serializable {
    init {
      require(numShards > 0)
      require(numBucketsPerShard > 0)
    }
  }

  /** Applies query encryption to a set of already encrypted join keys for use in private
   * information retrieval. The encrypted join keys are the output of a separate join key exchange.
   */
  fun encryptQueriesofJoinKeys(
    encryptedJoinKeys: PCollection<ByteString>
  ): PCollection<Result> {
    val bucketing = Bucketing(parameters.numShards, parameters.numBucketsPerShard)
    return encryptedJoinKeys
      .pardo {
        yield hashSha256(it)
      }
    val hashedEncryptedJoinKeys: PCollection<KV<ShardId, DatabaseShard>> = hashJoinKeys(encryptedJoinKeys)

    val queriesByShard = queryBundles.keyBy("Key QueryBundles by Shard") { it.shardId }

    val uncombinedResults: PCollection<KV<KV<ShardId, QueryId>, Result>> =
      querySubshards(shardedDatabase, queriesByShard)

    return uncombinedResults
      .combinePerKey("Combine Subshard Results") { queryEvaluator.combineResults(it.asSequence()) }
      .values("Extract Results")
  }

  /** Hashes a list [encryptedJoinKeys]. */
  private fun hashJoinKeys(
    encryptedJoinKeys: PCollection<ByteString>
  ): PCollection<KV<ShardId, DatabaseShard>> {

  }

  /** Enriches a list [hashedEncryptedJoinKeys] with shard, bucket and query ids. */
  private fun shardJoinKeys(
    hashedEncryptedJoinKeys: PCollection<ByteString>
  ): PCollection<KV<ShardId, DatabaseShard>> {
    val bucketing = Bucketing(parameters.numShards, parameters.numBucketsPerShard)
    return database
      .pardo {
        val (shardId, bucketId) = bucketing.apply(it) hashSha256(it)
      }
      .keyBy {

        kvOf(shardId, bucketId)
      }
      .groupByKey("Group by Shard")
  }
}
