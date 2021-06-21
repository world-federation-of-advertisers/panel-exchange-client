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

data class ShardId(val id: Int)

data class BucketId(val id: Int)

data class Bucket(val bucketId: BucketId, val data: ByteString)

data class QueryId(val id: Int)

data class QueryBundle(val shardId: ShardId, val queryIds: List<QueryId>, val data: ByteString)

data class DatabaseShard(val shardId: ShardId, val buckets: List<Bucket>)

data class Result(val queryId: QueryId, val data: ByteString) {
  override fun toString(): String {
    if (data.isValidUtf8) {
      return "Result(queryId=${queryId.id}, data='${data.toStringUtf8()}')"
    }
    return "Result(queryId=${queryId.id}, data=${data.toByteArray()})"
  }
}

/** Provides core batch lookup operations. */
interface BatchLookupClient : Serializable {
  /** Executes [queryBundles] on [shards]. */
  fun executeQueries(shards: List<DatabaseShard>, queryBundles: List<QueryBundle>): List<Result>

  /** Combines [results] into a single [Result]. Throws if results have different queryIds. */
  fun combineResults(results: Sequence<Result>): Result
}
