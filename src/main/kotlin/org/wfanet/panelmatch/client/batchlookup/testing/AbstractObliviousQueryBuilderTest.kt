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
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.wfanet.panelmatch.client.batchlookup.EncryptQueriesRequest
import org.wfanet.panelmatch.client.batchlookup.DecryptQueriesRequest
import org.wfanet.panelmatch.client.batchlookup.ObliviousQueryBuilder
import org.wfanet.panelmatch.client.batchlookup.QueryBundle
import org.wfanet.panelmatch.client.batchlookup.bucketIdOf
import org.wfanet.panelmatch.client.batchlookup.queryBundleOf
import org.wfanet.panelmatch.client.batchlookup.queryIdOf
import org.wfanet.panelmatch.client.batchlookup.shardIdOf
import org.wfanet.panelmatch.client.batchlookup.unencryptedQueryOf

abstract class AbstractObliviousQueryBuilderTest {
  abstract val obliviousQueryBuilder: ObliviousQueryBuilder

  @Test
  fun `encryptQueries with multiple shards`() {
    val encryptQueriesRequest =
      EncryptQueriesRequest.newBuilder()
        .addAllUnencryptedQuery(
          listOf(
            unencryptedQueryOf(100, 1, 1),
            unencryptedQueryOf(100, 2, 2),
            unencryptedQueryOf(101, 3, 1),
            unencryptedQueryOf(101, 4, 5)
          )
        )
        .build()
    val encryptedQueries = obliviousQueryBuilder.encryptQueries(encryptQueriesRequest)
    assertThat(encryptedQueries.getCiphertextsList().map { it -> QueryBundle.parseFrom(it) })
      .containsExactly(
        queryBundleOf(shard = 100, listOf(1 to 1, 2 to 2)),
        queryBundleOf(shard = 101, listOf(3 to 1, 4 to 5))
      )
  }

  @Test
  fun `decryptQueries`() {
    val queriedData = listOf(
      "<this is the payload for -3026910158441561776>",
      "<this is the payload for -4228343866523403918>",
      "<this is the payload for -6367288518484839692>",
      "<this is the payload for -6428220448289816081>",
      "<this is the payload for -3350035376205273921>",
      "<this is the payload for 7288696731122253832>",
      "<this is the payload for -8481730907691591520>",
      "<this is the payload for -2252118164433982605>",
      "<this is the payload for -4359497035184897174>",
      "<this is the payload for 4129736453975454884>"
    )
    val decryptQueriesRequest =
      DecryptQueriesRequest.newBuilder()
        .addAllEncryptedQueryResults(listOf(ByteString.copyFromUtf8(queriedData.joinToString(""))))
        .build()
    val decryptedQueries = obliviousQueryBuilder.decryptQueries(decryptQueriesRequest)
    assertThat(decryptedQueries.getDecryptedQueryResultsList().map { it.toStringUtf8() }.toSet())
      .containsExactlyElementsIn(queriedData.toSet())
  }
}

private fun queryBundleOf(shard: Int, queries: List<Pair<Int, Int>>): QueryBundle {
  return PlaintextQueryEvaluatorTestHelper.makeQueryBundle(
    shardIdOf(shard),
    queries.map { queryIdOf(it.first) to bucketIdOf(it.second) }
  )
}
