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

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import java.lang.IllegalArgumentException
import kotlin.test.assertFailsWith
import org.junit.Test
import org.wfanet.panelmatch.client.batchlookup.Bucket
import org.wfanet.panelmatch.client.batchlookup.BucketId
import org.wfanet.panelmatch.client.batchlookup.DatabaseShard
import org.wfanet.panelmatch.client.batchlookup.QueryBundle
import org.wfanet.panelmatch.client.batchlookup.QueryEvaluator
import org.wfanet.panelmatch.client.batchlookup.QueryId
import org.wfanet.panelmatch.client.batchlookup.Result
import org.wfanet.panelmatch.client.batchlookup.ShardId

/** Tests for [QueryEvaluator]s. */
abstract class AbstractQueryEvaluatorTest {
  /** Provides a test subject. */
  abstract val evaluator: QueryEvaluator

  abstract val helper: QueryEvaluatorTestHelper

  @Test
  fun executeQueries() {
    val database =
      listOf(
        databaseShardOf(shard = 100, buckets = listOf(0, 1, 2)),
        databaseShardOf(shard = 101, buckets = listOf(3, 4, 5))
      )
    val queryBundles =
      listOf(
        queryBundleOf(shard = 100, queries = listOf(500 to 0, 501 to 1, 502 to 3)),
        queryBundleOf(shard = 100, queries = listOf(503 to 9)),
        queryBundleOf(shard = 102, queries = listOf(504 to 0, 505 to 1, 506 to 2, 507 to 3))
      )
    val results = evaluator.executeQueries(database, queryBundles)
    assertThat(results.map { it.queryMetadata.queryId to helper.decodeResultData(it) })
      .containsExactly(
        QueryId(500) to makeFakeBucketData(bucket = 0, shard = 100),
        QueryId(501) to makeFakeBucketData(bucket = 1, shard = 100),
        QueryId(502) to ByteString.EMPTY,
        QueryId(503) to ByteString.EMPTY
      )
  }

  @Test
  fun `executeQueries same bucket with multiple shards`() {
    val database =
      listOf(
        databaseShardOf(shard = 100, buckets = listOf(1)),
        databaseShardOf(shard = 101, buckets = listOf(1))
      )

    val queryBundles =
      listOf(
        queryBundleOf(shard = 100, queries = listOf(500 to 1)),
        queryBundleOf(shard = 101, queries = listOf(501 to 1))
      )

    val results = evaluator.executeQueries(database, queryBundles)
    assertThat(results.map { it.queryMetadata.queryId to helper.decodeResultData(it) })
      .containsExactly(
        QueryId(500) to makeFakeBucketData(bucket = 1, shard = 100),
        QueryId(501) to makeFakeBucketData(bucket = 1, shard = 101)
      )
  }

  @Test
  fun `combineResults empty input`() {
    assertFailsWith<IllegalArgumentException> { evaluator.combineResults(emptySequence()) }
  }

  @Test
  fun `combineResults mismatching queries`() {
    assertFailsWith<IllegalArgumentException> {
      runCombineResults(resultOf(1, ByteString.EMPTY), resultOf(2, ByteString.EMPTY))
    }
  }

  @Test
  fun `combineResults single result`() {
    assertThat(runCombineResults(resultOf(5, ByteString.EMPTY)))
      .isEqualTo(QueryId(5) to ByteString.EMPTY)

    val rawPayload = "abcdef".toByteString()
    assertThat(runCombineResults(resultOf(5, rawPayload))).isEqualTo(QueryId(5) to rawPayload)
  }

  private fun runCombineResults(vararg results: Result): Pair<QueryId, ByteString> {
    val result = evaluator.combineResults(results.asSequence())
    return result.queryMetadata.queryId to helper.decodeResultData(result)
  }

  private fun queryBundleOf(shard: Int, queries: List<Pair<Int, Int>>): QueryBundle {
    return helper.makeQueryBundle(
      ShardId(shard),
      queries.map { QueryId(it.first) to BucketId(it.second) }
    )
  }

  private fun resultOf(query: Int, rawPayload: ByteString): Result {
    return helper.makeResult(QueryId(query), rawPayload)
  }
}

private fun bucketOf(id: Int, data: ByteString): Bucket {
  return Bucket(BucketId(id), data)
}

private fun databaseShardOf(shard: Int, buckets: List<Int>): DatabaseShard {
  return DatabaseShard(ShardId(shard), buckets.map { bucketOf(it, makeFakeBucketData(it, shard)) })
}

private fun makeFakeBucketData(bucket: Int, shard: Int): ByteString {
  return "bucket:$bucket-shard:$shard".toByteString()
}

private fun String.toByteString(): ByteString {
  return ByteString.copyFromUtf8(this)
}