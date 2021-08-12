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
import com.google.protobuf.ListValue
import kotlin.collections.zip
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.panelmatch.client.batchlookup.CreateQueriesWorkflow.Parameters
import org.wfanet.panelmatch.client.batchlookup.testing.PlaintextObliviousQueryBuilder
import org.wfanet.panelmatch.client.batchlookup.testing.PlaintextQueryEvaluatorTestHelper
import org.wfanet.panelmatch.common.beam.kvOf
import org.wfanet.panelmatch.common.beam.testing.BeamTestBase
import org.wfanet.panelmatch.common.beam.testing.assertThat

@RunWith(JUnit4::class)
class CreateQueriesWorkflowTest : BeamTestBase() {
  private val database by lazy {
    databaseOf(53L to "abc", 58L to "def", 71L to "hij", 85L to "klm", 95L to "nop", 99L to "qrs")
  }
  private val obliviousQueryBuilder = PlaintextObliviousQueryBuilder

  private fun runWorkflow(
    obliviousQueryBuilder: ObliviousQueryBuilder,
    parameters: Parameters
  ): PCollection<EncryptQueriesResponse> {
    return CreateQueriesWorkflow(
        parameters = parameters,
        obliviousQueryBuilder = obliviousQueryBuilder,
      )
      .createQueries(database)
  }

  @Test
  fun `Two Shards`() {
    val parameters = Parameters(numShards = 2, numBucketsPerShard = 5)
    val results = runWorkflow(obliviousQueryBuilder, parameters)
    assertThat(results).satisfies {
      assertThat(
          it
            .map { it.getCiphertextsList().map { QueryBundle.parseFrom(it) } }
            .flatten()
            .map { decodeQueryBundle(it) }
            .flatten()
        )
        .containsExactlyElementsIn(
          listOf(
            ShardedQuery(1, 5800L, 1),
            ShardedQuery(1, 9500L, 2),
            ShardedQuery(1, 8500L, 1),
            ShardedQuery(1, 7100L, 3),
            ShardedQuery(0, 5300L, 0),
            ShardedQuery(0, 9900L, 0),
          )
        )
      null
    }
  }

  private fun databaseOf(
    vararg entries: Pair<Long, String>
  ): PCollection<KV<PanelistKey, JoinKey>> {
    return pcollectionOf(
      "Create Database",
      *entries
        .map {
          kvOf(panelistKeyOf(it.first.toLong()), joinKeyOf(ByteString.copyFromUtf8(it.second)))
        }
        .toTypedArray()
    )
  }
}

private data class ShardedQuery(val shard: Int, val query: Long, val bucket: Int) {
  var shardId = shardIdOf(shard)
  var queryId = queryIdOf(query)
  var bucketId = bucketIdOf(bucket)
}

private fun queryBundleOf(shard: Int, queries: List<Pair<Long, Int>>): QueryBundle {
  return PlaintextQueryEvaluatorTestHelper.makeQueryBundle(
    shardIdOf(shard),
    queries.map { queryIdOf(it.first) to bucketIdOf(it.second) }
  )
}

private fun decodeQueryBundle(queryBundle: QueryBundle): List<ShardedQuery> {
  val queryBundleList = queryBundle.getQueryMetadataList()
  val bucketValuesList =
    ListValue.parseFrom(queryBundle.payload).getValuesList().map { it.stringValue.toInt() }
  return queryBundleList.zip(bucketValuesList) { a: QueryMetadata, b: Int ->
    ShardedQuery(requireNotNull(queryBundle.shardId).id, a.queryId.id, b)
  }
}
