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
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.panelmatch.client.common.bucketIdOf
import org.wfanet.panelmatch.client.common.databaseKeyOf
import org.wfanet.panelmatch.client.common.plaintextOf
import org.wfanet.panelmatch.client.common.queryIdOf
import org.wfanet.panelmatch.client.common.shardIdOf
import org.wfanet.panelmatch.client.privatemembership.testing.PlaintextQueryEvaluator
import org.wfanet.panelmatch.client.privatemembership.testing.PlaintextQueryEvaluatorTestHelper
import org.wfanet.panelmatch.common.beam.kvOf
import org.wfanet.panelmatch.common.beam.testing.BeamTestBase
import org.wfanet.panelmatch.common.beam.testing.assertThat
import org.wfanet.panelmatch.common.toByteString

@RunWith(JUnit4::class)
class EvaluateQueriesTest : BeamTestBase() {
  private val database by lazy {
    databaseOf(53L to "abc", 58L to "def", 71L to "ghi", 85L to "jkl")
  }

  private fun runWorkflow(
    queryBundles: List<EncryptedQueryBundle>,
    parameters: EvaluateQueriesParameters
  ): PCollection<EncryptedQueryResult> {
    return evaluateQueries(
      database,
      pcollectionOf("Create Query Bundles", queryBundles),
      pcollectionViewOf(
        "Create SerializedPublicKey",
        PlaintextQueryEvaluatorTestHelper.serializedPublicKey
      ),
      parameters,
      PlaintextQueryEvaluator
    )
  }

  @Test
  fun `single EncryptedQueryBundle`() {
    // With `parameters`, we expect database to have:
    //  - Shard 0 to have bucket 4 with "def" in it
    //  - Shard 1 to have buckets 0 with "ghi", 1 with "abc", and 2 with "jkl"
    val parameters =
      EvaluateQueriesParameters(numShards = 2, numBucketsPerShard = 5, maxQueriesPerShard = 5)

    val queryBundles =
      listOf(encryptedQueryBundleOf(shard = 1, listOf(100 to 0, 101 to 0, 102 to 1)))

    assertThat(runWorkflow(queryBundles, parameters))
      .containsInAnyOrder(resultOf(100, "ghi"), resultOf(101, "ghi"), resultOf(102, "abc"))
  }

  @Test
  fun `multiple shards`() {
    // With `parameters`, we expect database to have:
    //  - Shard 0 to have bucket 4 with "def" in it
    //  - Shard 1 to have buckets 0 with "ghi", 1 with "abc", and 2 with "jkl"
    val parameters =
      EvaluateQueriesParameters(numShards = 2, numBucketsPerShard = 5, maxQueriesPerShard = 10)

    val queryBundles =
      listOf(
        encryptedQueryBundleOf(shard = 0, listOf(100 to 4)),
        encryptedQueryBundleOf(shard = 1, listOf(101 to 0, 102 to 0, 103 to 1))
      )

    assertThat(runWorkflow(queryBundles, parameters))
      .containsInAnyOrder(
        resultOf(100, "def"),
        resultOf(101, "ghi"),
        resultOf(102, "ghi"),
        resultOf(103, "abc")
      )
  }

  @Test
  fun `multiple bundles for one shard`() {
    // With `parameters`, we expect database to have:
    //  - Shard 0 to have bucket 4 with "def" in it
    //  - Shard 1 to have buckets 0 with "ghi", 1 with "abc", and 2 with "jkl"
    val parameters =
      EvaluateQueriesParameters(numShards = 2, numBucketsPerShard = 5, maxQueriesPerShard = 10)

    val queryBundles =
      listOf(
        encryptedQueryBundleOf(shard = 1, listOf(100 to 0, 101 to 1, 102 to 2)),
        encryptedQueryBundleOf(shard = 1, listOf(103 to 0)),
        encryptedQueryBundleOf(shard = 1, listOf(104 to 1)),
      )

    assertThat(runWorkflow(queryBundles, parameters))
      .containsInAnyOrder(
        resultOf(100, "ghi"),
        resultOf(101, "abc"),
        resultOf(102, "jkl"),
        resultOf(103, "ghi"),
        resultOf(104, "abc")
      )
  }

  @Test
  fun `repeated bucket`() {
    val parameters =
      EvaluateQueriesParameters(numShards = 1, numBucketsPerShard = 1, maxQueriesPerShard = 1)

    val queryBundles = listOf(encryptedQueryBundleOf(shard = 0, listOf(17 to 0)))

    assertThat(runWorkflow(queryBundles, parameters)).satisfies {
      val list = it.toList()
      assertThat(list).hasSize(1)
      assertThat(list[0].queryId).isEqualTo(queryIdOf(17))
      val bucketContents = BucketContents.parseFrom(list[0].serializedEncryptedQueryResult)
      assertThat(bucketContents.itemsList.map(ByteString::toStringUtf8))
        .containsExactly("abc", "def", "ghi", "jkl")
      null
    }
  }

  @Test
  fun `too many queries per shard`() {
    // With `parameters`, we expect database to have:
    //  - Shard 0 to have bucket 4 with "def" in it
    //  - Shard 1 to have buckets 0 with "ghi", 1 with "abc", and 2 with "jkl"
    val parameters =
      EvaluateQueriesParameters(numShards = 2, numBucketsPerShard = 5, maxQueriesPerShard = 2)

    val queryBundles =
      listOf(encryptedQueryBundleOf(shard = 1, listOf(100 to 0, 101 to 0, 102 to 1)))

    runWorkflow(queryBundles, parameters)

    assertFails { pipeline.run() }
  }

  private fun databaseOf(
    vararg entries: Pair<Long, String>
  ): PCollection<KV<DatabaseKey, Plaintext>> {
    return pcollectionOf(
      "Create Database",
      entries.map { kvOf(databaseKeyOf(it.first), plaintextOf(it.second.toByteString())) }
    )
  }
}

private fun resultOf(query: Int, rawPayload: String): EncryptedQueryResult {
  return PlaintextQueryEvaluatorTestHelper.makeResult(
    queryIdOf(query),
    bucketContents { items += rawPayload.toByteString() }.toByteString()
  )
}

private fun encryptedQueryBundleOf(
  shard: Int,
  queries: List<Pair<Int, Int>>
): EncryptedQueryBundle {
  return PlaintextQueryEvaluatorTestHelper.makeQueryBundle(
    shardIdOf(shard),
    queries.map { queryIdOf(it.first) to bucketIdOf(it.second) }
  )
}