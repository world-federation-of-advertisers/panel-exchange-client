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

/** Strongly typed shard identifier. */
data class ShardId(val id: Int)

/** Strongly typed bucket identifier. */
data class BucketId(val id: Int)

/** An input database bucket. */
data class Bucket(val bucketId: BucketId, val data: ByteString) {
  override fun toString(): String {
    return "Bucket(id=${bucketId.id}, data=${data.toDebugString()})"
  }
}

/** Strongly typed queryMetadata identifier. */
data class QueryId(val id: Int)

/** Metadata associated with a query. */
data class QueryMetadata(val queryId: QueryId, val metadata: ByteString) {
  override fun toString(): String {
    return "QueryMetadata(queryId=${queryId.id}, metadata=${metadata.toDebugString()})"
  }
}

/**
 * A collection of queries all belonging to one shard that are packed into ciphertexts.
 *
 * @property shardId the shard all the queries belong to
 * @property queryMetadata metadata for the queries
 * @property data serialized ciphertexts into which the query data itself is packed
 */
data class QueryBundle(
  val shardId: ShardId,
  val queryMetadata: List<QueryMetadata>,
  val data: ByteString
)

/** Holds all or some of the buckets assigned to the same shard. */
data class DatabaseShard(val shardId: ShardId, val buckets: List<Bucket>) {
  override fun toString(): String {
    return "DatabaseShard(shard=${shardId.id}, buckets=$buckets)"
  }
}

/** The result of executing a query on some buckets. */
data class Result(val queryMetadata: QueryMetadata, val data: ByteString) {
  override fun toString(): String {
    return "Result(queryId=${queryMetadata.queryId.id}, data=${data.toDebugString()})"
  }
}

private fun ByteString.toDebugString(): String {
  if (isValidUtf8) return toStringUtf8()
  return toByteArray().toString()
}

/** Provides core batch lookup operations. */
interface BatchLookupQueryEvaluator : Serializable {
  /** Executes [queryBundles] on [shards]. */
  fun executeQueries(shards: List<DatabaseShard>, queryBundles: List<QueryBundle>): List<Result>

  /** Combines [results] into a single [Result]. Throws if results have different queryIds. */
  fun combineResults(results: Sequence<Result>): Result
}
