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

import com.google.protobuf.ByteString
import com.google.protobuf.ListValue
import java.io.Serializable
import kotlin.collections.zip
import org.apache.beam.sdk.transforms.View
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.apache.beam.sdk.values.PCollectionView
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.panelmatch.client.privatemembership.CreateQueriesWorkflow.Parameters
import org.wfanet.panelmatch.client.privatemembership.testing.PlaintextObliviousQueryBuilder
import org.wfanet.panelmatch.client.privatemembership.testing.PlaintextQueryEvaluatorTestHelper
import org.wfanet.panelmatch.common.beam.groupByKey
import org.wfanet.panelmatch.common.beam.keyBy
import org.wfanet.panelmatch.common.beam.kvOf
import org.wfanet.panelmatch.common.beam.map
import org.wfanet.panelmatch.common.beam.parDo
import org.wfanet.panelmatch.common.beam.parDoWithSideInput
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
  ): Pair<PCollection<KV<PanelistKey, QueryId>>, PCollection<EncryptQueriesResponse>> {
    return CreateQueriesWorkflow(
        parameters = parameters,
        obliviousQueryBuilder = obliviousQueryBuilder,
      )
      .batchCreateQueries(database)
  }

  @Test
  fun `Two Shards`() {
    val parameters = Parameters(numShards = 2, numBucketsPerShard = 5)
    val (panelistKeyQueryId, encryptedResults) = runWorkflow(obliviousQueryBuilder, parameters)
    val mapOfPanelistKeyQueryId: PCollectionView<Map<Int, Long>> =
      panelistKeyQueryId
        .keyBy { 1 }
        .groupByKey()
        .parDo<KV<Int, Iterable<KV<PanelistKey, QueryId>>>, Map<Int, Long>> {
          yield(it.value.asSequence().map { it.value.id to it.key.id }.toMap())
        }
        .apply(View.asSingleton())
    val queriesMappedBackToPanelistKey: PCollection<PanelistQuery> =
      encryptedResults.parDoWithSideInput(mapOfPanelistKeyQueryId) { encryptedData, panelistQueryMap
        ->
        {
          print(panelistQueryMap)
          encryptedData
            .getCiphertextsList()
            .map { QueryBundle.parseFrom(it) }
            .map { decodeQueryBundle(it) }
            .flatten()
            .asSequence()
            .forEach {
              PanelistQuery(
                it.shardId.id,
                requireNotNull(panelistQueryMap[it.queryId.id]),
                it.bucketId.id
              )
            }
        }
      }
    assertThat(queriesMappedBackToPanelistKey)
      .containsInAnyOrder(
        PanelistQuery(0, 53L, 0),
        PanelistQuery(1, 58L, 1),
        PanelistQuery(1, 71L, 3),
        PanelistQuery(1, 85L, 1),
        PanelistQuery(1, 95L, 2),
        PanelistQuery(0, 99L, 0)
      )
  }

  private fun databaseOf(
    vararg entries: Pair<Long, String>
  ): PCollection<KV<PanelistKey, JoinKey>> {
    return pcollectionOf(
      "Create Database",
      *entries
        .map { kvOf(panelistKeyOf(it.first), joinKeyOf(ByteString.copyFromUtf8(it.second))) }
        .toTypedArray()
    )
  }
}

private data class ShardedQuery(
  private val shard: Int,
  private val query: Int,
  private val bucket: Int
) : Serializable {
  val shardId = shardIdOf(shard)
  val queryId = queryIdOf(query)
  val bucketId = bucketIdOf(bucket)
}

private data class PanelistQuery(
  private val shard: Int,
  private val panelist: Long,
  private val bucket: Int
) : Serializable {
  val shardId = shardIdOf(shard)
  val panelistKey = panelistKeyOf(panelist)
  val bucketId = bucketIdOf(bucket)
}

private fun queryBundleOf(shard: Int, queries: List<Pair<Int, Int>>): QueryBundle {
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
