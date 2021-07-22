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

package org.wfanet.panelmatch.client.batchlookup.testing

import com.google.protobuf.ByteString
import com.google.protobuf.ListValue
import org.wfanet.panelmatch.client.batchlookup.DatabaseShard
import org.wfanet.panelmatch.client.batchlookup.QueryBundle
import org.wfanet.panelmatch.client.batchlookup.QueryEvaluator
import org.wfanet.panelmatch.client.batchlookup.Result

object PlaintextQueryEvaluator : QueryEvaluator {
  override fun executeQueries(
    shards: List<DatabaseShard>,
    queryBundles: List<QueryBundle>
  ): List<Result> {
    val results = mutableListOf<Result>()
    for (shard in shards) {
      for (bundle in queryBundles) {
        if (shard.shardId.id == bundle.shardId.id) {
          results.addAll(query(shard, bundle))
        }
      }
    }
    return results
  }

  override fun combineResults(results: Sequence<Result>): Result {
    val resultsList = results.toList()
    require(resultsList.isNotEmpty())

    val queryId = resultsList.first().queryMetadata.queryId.id

    for (result in resultsList) {
      require(result.queryMetadata.queryId.id == queryId)
    }

    return resultsList.singleOrNull { !it.data.isEmpty } ?: resultsList.first()
  }

  private fun query(shard: DatabaseShard, bundle: QueryBundle): List<Result> {
    val queriedBuckets = ListValue.parseFrom(bundle.data)
    require(queriedBuckets.valuesCount == bundle.queryMetadata.size)
    val results = mutableListOf<Result>()
    for ((metadata, queriedBucket) in bundle.queryMetadata zip queriedBuckets.valuesList) {
      val result =
        shard
          .buckets
          .filter { it.bucketId.id == queriedBucket.stringValue.toInt() }
          .map { Result(metadata, it.data) }
          .ifEmpty { listOf(Result(metadata, ByteString.EMPTY)) }
          .single()
      results.add(result)
    }
    return results
  }
}
