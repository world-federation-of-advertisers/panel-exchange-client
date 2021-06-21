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

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.panelmatch.common.beam.kvOf
import org.wfanet.panelmatch.common.beam.testing.BeamTestBase
import org.wfanet.panelmatch.common.beam.testing.assertThat

@RunWith(JUnit4::class)
class PipelineTest : BeamTestBase() {
  private fun makeWorkflow(
    parameters: Parameters = Parameters(100, 100, 100)
  ): BatchLookupWorkflow {
    return BatchLookupWorkflow(parameters, FakeBatchLookupClient)
  }

  @Before
  fun registerCoders() {
    pipeline.registerPirCoders()
  }

  @Test
  fun batchLookup() {
    val parameters = Parameters(2, 5, 1000)

    // With `parameters`, we expect:
    //  - Shard 0 to have bucket 4 with "def" in it
    //  - Shard 1 to have buckets 0 with "hij", 1 with "abc", and 2 with "klm"
    val database = databaseOf(53 to "abc", 58 to "def", 71 to "hij", 85 to "klm")

    val queryBundles =
      pcollectionOf(
        "Create Query Bundles",
        queryBundleOf(shard = 0, data = "query-bundle-1", queryIds = listOf(100, 101)),
        queryBundleOf(shard = 1, data = "query-bundle-2", queryIds = listOf(102, 103))
      )

    val result: PCollection<Result> = makeWorkflow(parameters).batchLookup(database, queryBundles)

    assertThat(result)
      .containsInAnyOrder(
        makeFakeQueryResult(100, "query-bundle-1", listOf("4=def")),
        makeFakeQueryResult(101, "query-bundle-1", listOf("4=def")),
        makeFakeQueryResult(102, "query-bundle-2", listOf("1=abc", "0=hij", "2=klm")),
        makeFakeQueryResult(103, "query-bundle-2", listOf("1=abc", "0=hij", "2=klm"))
      )
  }

  @Test
  fun querySubshards() {
    val shardedDatabase =
      pcollectionOf(
        "Create Sharded Database",
        shardedDatabaseKvOf(5, bucketOf(10, "bucket-5-10"), bucketOf(11, "bucket-5-11")),
        shardedDatabaseKvOf(5, bucketOf(12, "bucket-5-12")),
        shardedDatabaseKvOf(6, bucketOf(13, "bucket-6-13"))
      )

    val queries =
      pcollectionOf(
        "Create Sharded QueryBundles",
        shardedQueryBundleOf(shard = 4, data = "shard-not-in-db", queryIds = listOf(100, 101, 102)),
        shardedQueryBundleOf(shard = 5, data = "bundle-has-no-queries", queryIds = emptyList()),
        shardedQueryBundleOf(shard = 5, data = "abc", queryIds = listOf(103, 104)),
        shardedQueryBundleOf(shard = 6, data = "def", queryIds = listOf(105, 106))
      )

    val results: PCollection<KV<KV<ShardId, QueryId>, Result>> =
      makeWorkflow().querySubshards(shardedDatabase, queries)

    assertThat(results)
      .containsInAnyOrder(
        unaggregatedResultOf(
          shard = 5,
          query = 103,
          bundleData = "abc",
          buckets = listOf("10=bucket-5-10", "11=bucket-5-11")
        ),
        unaggregatedResultOf(
          shard = 5,
          query = 103,
          bundleData = "abc",
          buckets = listOf("12=bucket-5-12")
        ),
        unaggregatedResultOf(
          shard = 5,
          query = 104,
          bundleData = "abc",
          buckets = listOf("10=bucket-5-10", "11=bucket-5-11")
        ),
        unaggregatedResultOf(
          shard = 5,
          query = 104,
          bundleData = "abc",
          buckets = listOf("12=bucket-5-12")
        ),
        unaggregatedResultOf(
          shard = 6,
          query = 105,
          bundleData = "def",
          buckets = listOf("13=bucket-6-13")
        ),
        unaggregatedResultOf(
          shard = 6,
          query = 106,
          bundleData = "def",
          buckets = listOf("13=bucket-6-13")
        )
      )
  }

  @Test
  fun shardDatabase() {
    val database = databaseOf(53 to "abc", 58 to "def", 71 to "hij", 85 to "klm")

    fun runWithParameters(parameters: Parameters): PCollection<KV<ShardId, DatabaseShard>> {
      return makeWorkflow(parameters).shardDatabase(database)
    }

    // Large subshard sizes.
    assertThat(runWithParameters(Parameters(2, 5, 1000))).satisfies { results ->
      assertThat(canonicalize(results))
        .containsExactly(
          shardedDatabaseKvOf(0, bucketOf(4, "def")),
          shardedDatabaseKvOf(1, bucketOf(0, "hij"), bucketOf(1, "abc"), bucketOf(2, "klm"))
        )
      null
    }

    // Medium subshard sizes.
    assertThat(runWithParameters(Parameters(2, 5, 7))).satisfies { results ->
      assertThat(results).hasSize(3)
      assertThat(canonicalize(results))
        .containsAnyOf(
          shardedDatabaseKvOf(1, bucketOf(0, "hij"), bucketOf(1, "abc")),
          shardedDatabaseKvOf(1, bucketOf(0, "hij"), bucketOf(2, "klm")),
          shardedDatabaseKvOf(1, bucketOf(1, "abc"), bucketOf(2, "klm"))
        )
      null
    }

    // Small subshard sizes.
    assertThat(runWithParameters(Parameters(2, 5, 3))).satisfies { results ->
      assertThat(canonicalize(results))
        .containsExactly(
          shardedDatabaseKvOf(0, bucketOf(4, "def")),
          shardedDatabaseKvOf(1, bucketOf(0, "hij")),
          shardedDatabaseKvOf(1, bucketOf(1, "abc")),
          shardedDatabaseKvOf(1, bucketOf(2, "klm"))
        )
      null
    }
  }

  private fun databaseOf(
    vararg entries: Pair<Int, String>
  ): PCollection<KV<DatabaseKey, Plaintext>> {
    return pcollectionOf(
      "Create Database",
      *entries
        .map { kvOf(DatabaseKey(it.first.toLong()), Plaintext(ByteString.copyFromUtf8(it.second))) }
        .toTypedArray()
    )
  }
}

private fun shardedDatabaseKvOf(shard: Int, vararg buckets: Bucket): KV<ShardId, DatabaseShard> {
  return kvOf(ShardId(shard), DatabaseShard(ShardId(shard), buckets.toList()))
}

private fun shardedQueryBundleOf(
  shard: Int,
  data: String,
  queryIds: List<Int>
): KV<ShardId, QueryBundle> {
  return kvOf(ShardId(shard), queryBundleOf(shard, data, queryIds))
}

private fun unaggregatedResultOf(
  shard: Int,
  query: Int,
  bundleData: String,
  buckets: List<String>
): KV<KV<ShardId, QueryId>, Result> {
  val result = makeFakeQueryResult(query, bundleData, buckets)
  return kvOf(kvOf(ShardId(shard), result.queryId), result)
}

private fun bucketOf(id: Int, contents: String): Bucket {
  return Bucket(BucketId(id), ByteString.copyFromUtf8(contents))
}

private fun queryBundleOf(shard: Int, data: String, queryIds: List<Int>): QueryBundle {
  return QueryBundle(ShardId(shard), queryIds.map(::QueryId), ByteString.copyFromUtf8(data))
}

private fun canonicalize(
  results: Iterable<KV<ShardId, DatabaseShard>>
): List<KV<ShardId, DatabaseShard>> {
  return results.map { result ->
    shardedDatabaseKvOf(
      result.key.id,
      *result.value.buckets.sortedBy { it.bucketId.id }.toTypedArray()
    )
  }
}

private fun makeFakeQueryResult(query: Int, bundleData: String, buckets: Iterable<String>): Result {
  val bucketsString = buckets.sorted().joinToString(", ")
  val resultString = "QueryResult(query=$query, bundleData=$bundleData, buckets=[$bucketsString])"
  return Result(QueryId(query), ByteString.copyFromUtf8(resultString))
}

/**
 * This [BatchLookupClient] implementation assuming ByteArrays are UTF8 strings of plaintext
 * numbers.
 */
private object FakeBatchLookupClient : BatchLookupClient {
  @OptIn(ExperimentalStdlibApi::class) // For buildList
  override fun executeQueries(
    shards: List<DatabaseShard>,
    queryBundles: List<QueryBundle>
  ): List<Result> = buildList {
    for (shard in shards) {
      val buckets = shard.buckets.map { "${it.bucketId.id}=${it.data.toStringUtf8()}" }
      for (bundle in queryBundles) {
        val bundleData = bundle.data.toStringUtf8()
        for (queryId in bundle.queryIds) {
          add(makeFakeQueryResult(queryId.id, bundleData, buckets))
        }
      }
    }
  }

  override fun combineResults(results: Sequence<Result>): Result {
    val resultsList = results.toList()
    val combined = resultsList.map { it.data.toStringUtf8() }.sorted().joinToString(", ")
    return Result(resultsList[0].queryId, ByteString.copyFromUtf8(combined))
  }
}
