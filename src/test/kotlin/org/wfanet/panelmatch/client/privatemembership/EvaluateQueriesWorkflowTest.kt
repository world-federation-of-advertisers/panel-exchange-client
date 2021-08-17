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
import org.wfanet.panelmatch.client.privatemembership.EvaluateQueriesWorkflow.Parameters
import org.wfanet.panelmatch.client.privatemembership.testing.PlaintextQueryEvaluator
import org.wfanet.panelmatch.client.privatemembership.testing.PlaintextQueryEvaluatorTestHelper
import org.wfanet.panelmatch.common.beam.kvOf
import org.wfanet.panelmatch.common.beam.testing.BeamTestBase
import org.wfanet.panelmatch.common.beam.testing.assertThat

@RunWith(JUnit4::class)
class EvaluateQueriesWorkflowTest : BeamTestBase() {
  private val database by lazy {
    databaseOf(53L to "abc", 58L to "def", 71L to "hij", 85L to "klm")
  }

  private fun runWorkflow(
    queryBundles: List<QueryBundle>,
    parameters: Parameters
  ): PCollection<Result> {
    return EvaluateQueriesWorkflow(parameters, PlaintextQueryEvaluator)
      .batchEvaluateQueries(
        database,
        pcollectionOf("Create Query Bundles", *queryBundles.toTypedArray())
      )
  }

  @Test
  fun `single QueryBundle`() {
    // With `parameters`, we expect database to have:
    //  - Shard 0 to have bucket 4 with "def" in it
    //  - Shard 1 to have buckets 0 with "hij", 1 with "abc", and 2 with "klm"
    val parameters = Parameters(numShards = 2, numBucketsPerShard = 5, subshardSizeBytes = 1000)

    val queryBundles = listOf(queryBundleOf(shard = 1, listOf(100 to 0, 101 to 0, 102 to 1)))

    assertThat(runWorkflow(queryBundles, parameters))
      .containsInAnyOrder(resultOf(100, "hij"), resultOf(101, "hij"), resultOf(102, "abc"))

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
        queryBundleOf(shard = 0, listOf(100 to 4)),
        queryBundleOf(shard = 1, listOf(101 to 0, 102 to 0, 103 to 1)),
        queryBundleOf(shard = 2, listOf(104 to 0)),
      )

    assertThat(runWorkflow(queryBundles, parameters))
      .containsInAnyOrder(
        resultOf(100, "def"),
        resultOf(101, "hij"),
        resultOf(102, "hij"),
        resultOf(103, "abc"),
        resultOf(104, "")
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
        queryBundleOf(shard = 1, listOf(100 to 0, 101 to 1, 102 to 2)),
        queryBundleOf(shard = 1, listOf(103 to 0)),
        queryBundleOf(shard = 1, listOf(104 to 1)),
      )

    assertThat(runWorkflow(queryBundles, parameters))
      .containsInAnyOrder(
        resultOf(100, "hij"),
        resultOf(101, "abc"),
        resultOf(102, "klm"),
        resultOf(103, "hij"),
        resultOf(104, "abc")
      )

    assertThat(runPipelineAndGetActualMaxSubshardSize()).isEqualTo(9)
  }

  @Test
  fun `subshardSizeBytes too small`() {
    val parameters = Parameters(numShards = 1, numBucketsPerShard = 100, subshardSizeBytes = 2)

    val queryBundles =
      listOf(
        queryBundleOf(shard = 0, listOf(1 to 53, 2 to 58)),
        queryBundleOf(shard = 0, listOf(3 to 71, 4 to 85))
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
        queryBundleOf(shard = 0, listOf(1 to 53, 2 to 58)),
        queryBundleOf(shard = 0, listOf(3 to 71, 4 to 85))
      )

    assertThat(runWorkflow(queryBundles, parameters))
      .containsInAnyOrder(
        resultOf(1, "abc"),
        resultOf(2, "def"),
        resultOf(3, "hij"),
        resultOf(4, "klm")
      )

    assertThat(runPipelineAndGetActualMaxSubshardSize()).isEqualTo(3)
  }

  @Test
  fun `subshards that fit two items`() {
    val parameters = Parameters(numShards = 1, numBucketsPerShard = 100, subshardSizeBytes = 8)

    val queryBundles =
      listOf(
        queryBundleOf(shard = 0, listOf(1 to 53, 2 to 58)),
        queryBundleOf(shard = 0, listOf(3 to 71, 4 to 85))
      )

    assertThat(runWorkflow(queryBundles, parameters))
      .containsInAnyOrder(
        resultOf(1, "abc"),
        resultOf(2, "def"),
        resultOf(3, "hij"),
        resultOf(4, "klm")
      )

    assertThat(runPipelineAndGetActualMaxSubshardSize()).isEqualTo(6)
  }

  @Test
  fun `repeated bucket`() {
    val parameters = Parameters(numShards = 1, numBucketsPerShard = 1, subshardSizeBytes = 1000)

    val queryBundles = listOf(queryBundleOf(shard = 0, listOf(17 to 0)))

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
    vararg entries: Pair<Long, String>
  ): PCollection<KV<DatabaseKey, Plaintext>> {
    return pcollectionOf(
      "Create Database",
      *entries
        .map { kvOf(databaseKeyOf(it.first), plaintextOf(it.second.toByteString())) }
        .toTypedArray()
    )
  }

  private fun runPipelineAndGetActualMaxSubshardSize(): Long {
    val pipelineResult = pipeline.run()
    pipelineResult.waitUntilFinish()
    val filter =
      MetricsFilter.builder()
        .addNameFilter(
          MetricNameFilter.named(EvaluateQueriesWorkflow::class.java, "database-shard-sizes")
        )
        .build()
    val metrics = pipelineResult.metrics().queryMetrics(filter)
    assertThat(metrics.distributions).isNotEmpty()
    return metrics.distributions.map { it.committed.max }.first()
  }
}

private fun resultOf(query: Int, rawPayload: String): Result {
  return PlaintextQueryEvaluatorTestHelper.makeResult(queryIdOf(query), rawPayload.toByteString())
}

private fun queryBundleOf(shard: Int, queries: List<Pair<Int, Int>>): QueryBundle {
  return PlaintextQueryEvaluatorTestHelper.makeQueryBundle(
    shardIdOf(shard),
    queries.map { queryIdOf(it.first) to bucketIdOf(it.second) }
  )
}

private fun String.toByteString(): ByteString {
  return ByteString.copyFromUtf8(this)
}