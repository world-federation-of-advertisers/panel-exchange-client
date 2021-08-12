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
import com.google.common.truth.extensions.proto.ProtoTruth.assertThat
import com.google.protobuf.ByteString
import kotlin.test.assertFails
import org.apache.beam.sdk.metrics.MetricNameFilter
import org.apache.beam.sdk.metrics.MetricsFilter
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.panelmatch.client.batchlookup.BatchLookupWorkflow.Parameters
import org.wfanet.panelmatch.client.batchlookup.testing.PlaintextQueryEvaluator
import org.wfanet.panelmatch.client.batchlookup.testing.PlaintextQueryEvaluatorTestHelper
import org.wfanet.panelmatch.common.beam.kvOf
import org.wfanet.panelmatch.common.beam.testing.BeamTestBase
import org.wfanet.panelmatch.common.beam.testing.assertThat

@RunWith(JUnit4::class)
class BatchLookupWorkflowTest : BeamTestBase() {
  private val database by lazy { databaseOf(53 to "abc", 58 to "def", 71 to "hij", 85 to "klm") }

  private fun runWorkflow(
    queryBundles: List<QueryBundle>,
    parameters: Parameters
  ): PCollection<Result> {
    return BatchLookupWorkflow(parameters, PlaintextQueryEvaluator)
      .batchLookup(database, pcollectionOf("Create Query Bundles", *queryBundles.toTypedArray()))
  }

  @Test
  fun `single QueryBundle`() {
    // With `parameters`, we expect database to have:
    //  - Shard 0 to have bucket 4 with "def" in it
    //  - Shard 1 to have buckets 0 with "hij", 1 with "abc", and 2 with "klm"
    val parameters = Parameters(numShards = 2, numBucketsPerShard = 5, subshardSizeBytes = 1000)

    val queryBundles = listOf(queryBundleOf(shard = 1, listOf(100L to 0, 101L to 0, 102L to 1)))

    assertThat(runWorkflow(queryBundles, parameters))
      .containsInAnyOrder(resultOf(100L, "hij"), resultOf(101L, "hij"), resultOf(102L, "abc"))

    assertThat(runPipelineAndGetActualMaxSubshardSize()).isEqualTo(9)
  }

  @Test
  fun `multiple shards`() {
    // With `parameters`, we expect database to have:
    //  - Shard 0 to have bucket 4 with "def" in it
    //  - Shard 1 to have buckets 0 with "hij", 1 with "abc", and 2 with "klm"
    val parameters = Parameters(numShards = 2, numBucketsPerShard = 5, subshardSizeBytes = 1000)

    val queryBundles =
      listOf(
        queryBundleOf(shard = 0, listOf(100L to 4)),
        queryBundleOf(shard = 1, listOf(101L to 0, 102L to 0, 103L to 1)),
        queryBundleOf(shard = 2, listOf(104L to 0)),
      )

    assertThat(runWorkflow(queryBundles, parameters))
      .containsInAnyOrder(
        resultOf(100L, "def"),
        resultOf(101L, "hij"),
        resultOf(102L, "hij"),
        resultOf(103L, "abc"),
        resultOf(104L, "")
      )

    assertThat(runPipelineAndGetActualMaxSubshardSize()).isEqualTo(9)
  }

  @Test
  fun `multiple bundles for one shard`() {
    // With `parameters`, we expect database to have:
    //  - Shard 0 to have bucket 4 with "def" in it
    //  - Shard 1 to have buckets 0 with "hij", 1 with "abc", and 2 with "klm"
    val parameters = Parameters(numShards = 2, numBucketsPerShard = 5, subshardSizeBytes = 1000)

    val queryBundles =
      listOf(
        queryBundleOf(shard = 1, listOf(100L to 0, 101L to 1, 102L to 2)),
        queryBundleOf(shard = 1, listOf(103L to 0)),
        queryBundleOf(shard = 1, listOf(104L to 1)),
      )

    assertThat(runWorkflow(queryBundles, parameters))
      .containsInAnyOrder(
        resultOf(100L, "hij"),
        resultOf(101L, "abc"),
        resultOf(102L, "klm"),
        resultOf(103L, "hij"),
        resultOf(104L, "abc")
      )

    assertThat(runPipelineAndGetActualMaxSubshardSize()).isEqualTo(9)
  }

  @Test
  fun `subshardSizeBytes too small`() {
    val parameters = Parameters(numShards = 1, numBucketsPerShard = 100, subshardSizeBytes = 2)

    val queryBundles =
      listOf(
        queryBundleOf(shard = 0, listOf(1L to 53, 2L to 58)),
        queryBundleOf(shard = 0, listOf(3L to 71, 4L to 85))
      )

    runWorkflow(queryBundles, parameters)

    // We expect the pipeline to crash if `subshardSizeBytes` is smaller than an individual bucket.
    assertFails { pipeline.run() }
  }

  @Test
  fun `subshards that fit a single item`() {
    // With `parameters`, we expect database to have a single shard with 5 subshards.
    // The buckets should be 53 to "abc", 58 to "def", 71 to "hij", 85 to "klm".
    val parameters = Parameters(numShards = 1, numBucketsPerShard = 100, subshardSizeBytes = 5)

    val queryBundles =
      listOf(
        queryBundleOf(shard = 0, listOf(1L to 53, 2L to 58)),
        queryBundleOf(shard = 0, listOf(3L to 71, 4L to 85))
      )

    assertThat(runWorkflow(queryBundles, parameters))
      .containsInAnyOrder(
        resultOf(1L, "abc"),
        resultOf(2L, "def"),
        resultOf(3L, "hij"),
        resultOf(4L, "klm")
      )

    assertThat(runPipelineAndGetActualMaxSubshardSize()).isEqualTo(3)
  }

  @Test
  fun `subshards that fit two items`() {
    val parameters = Parameters(numShards = 1, numBucketsPerShard = 100, subshardSizeBytes = 8)

    val queryBundles =
      listOf(
        queryBundleOf(shard = 0, listOf(1L to 53, 2L to 58)),
        queryBundleOf(shard = 0, listOf(3L to 71, 4L to 85))
      )

    assertThat(runWorkflow(queryBundles, parameters))
      .containsInAnyOrder(
        resultOf(1L, "abc"),
        resultOf(2L, "def"),
        resultOf(3L, "hij"),
        resultOf(4L, "klm")
      )

    assertThat(runPipelineAndGetActualMaxSubshardSize()).isEqualTo(6)
  }

  @Test
  fun `repeated bucket`() {
    val parameters = Parameters(numShards = 1, numBucketsPerShard = 1, subshardSizeBytes = 1000)

    val queryBundles = listOf(queryBundleOf(shard = 0, listOf(17L to 0)))

    assertThat(runWorkflow(queryBundles, parameters)).satisfies {
      val list = it.toList()
      assertThat(list).hasSize(1)
      assertThat(list[0].queryMetadata).isEqualTo(queryMetadataOf(queryIdOf(17), ByteString.EMPTY))
      assertThat(list[0].payload.toStringUtf8().toList())
        .containsExactlyElementsIn("abcdefhijklm".toList())
      null
    }
  }

  private fun databaseOf(
    vararg entries: Pair<Int, String>
  ): PCollection<KV<DatabaseKey, Plaintext>> {
    return pcollectionOf(
      "Create Database",
      *entries
        .map { kvOf(databaseKeyOf(it.first.toLong()), plaintextOf(it.second.toByteString())) }
        .toTypedArray()
    )
  }

  private fun runPipelineAndGetActualMaxSubshardSize(): Long {
    val pipelineResult = pipeline.run()
    pipelineResult.waitUntilFinish()
    val filter =
      MetricsFilter.builder()
        .addNameFilter(
          MetricNameFilter.named(BatchLookupWorkflow::class.java, "database-shard-sizes")
        )
        .build()
    val metrics = pipelineResult.metrics().queryMetrics(filter)
    assertThat(metrics.distributions).isNotEmpty()
    return metrics.distributions.map { it.committed.max }.first()
  }
}

private fun resultOf(query: Long, rawPayload: String): Result {
  return PlaintextQueryEvaluatorTestHelper.makeResult(queryIdOf(query), rawPayload.toByteString())
}

private fun queryBundleOf(shard: Int, queries: List<Pair<Long, Int>>): QueryBundle {
  return PlaintextQueryEvaluatorTestHelper.makeQueryBundle(
    shardIdOf(shard),
    queries.map { queryIdOf(it.first) to bucketIdOf(it.second) }
  )
}

private fun String.toByteString(): ByteString {
  return ByteString.copyFromUtf8(this)
}
