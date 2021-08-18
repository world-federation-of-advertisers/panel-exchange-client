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

package org.wfanet.panelmatch.client.privatemembership.testing

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import org.junit.Test
import org.wfanet.panelmatch.client.privatemembership.DecryptQueriesRequest
import org.wfanet.panelmatch.client.privatemembership.EncryptQueriesRequest
import org.wfanet.panelmatch.client.privatemembership.PrivateMembershipCryptor
import org.wfanet.panelmatch.client.privatemembership.QueryBundle
import org.wfanet.panelmatch.client.privatemembership.bucketIdOf
import org.wfanet.panelmatch.client.privatemembership.queryBundleOf
import org.wfanet.panelmatch.client.privatemembership.queryIdOf
import org.wfanet.panelmatch.client.privatemembership.queryMetadataOf
import org.wfanet.panelmatch.client.privatemembership.resultOf
import org.wfanet.panelmatch.client.privatemembership.shardIdOf
import org.wfanet.panelmatch.client.privatemembership.unencryptedQueryOf

class PlaintextObliviousQueryBuilderTest {
  private val privateMembershipCryptor: PrivateMembershipCryptor = PlaintextPrivateMembershipCryptor

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
    val encryptedQueries = privateMembershipCryptor.encryptQueries(encryptQueriesRequest)
    assertThat(encryptedQueries.getCiphertextsList().map { it -> QueryBundle.parseFrom(it) })
      .containsExactly(
        queryBundleOf(shard = 100, listOf(1 to 1, 2 to 2)),
        queryBundleOf(shard = 101, listOf(3 to 1, 4 to 5))
      )
  }

  @Test
  fun `decryptQueries`() {
    val queriedData =
      listOf(
        resultOf(
          queryMetadataOf(queryIdOf(1), ByteString.EMPTY),
          ByteString.copyFromUtf8("<some data a>")
        ),
        resultOf(
          queryMetadataOf(queryIdOf(2), ByteString.EMPTY),
          ByteString.copyFromUtf8("<some data b>")
        ),
        resultOf(
          queryMetadataOf(queryIdOf(3), ByteString.EMPTY),
          ByteString.copyFromUtf8("<some data c>")
        ),
        resultOf(
          queryMetadataOf(queryIdOf(4), ByteString.EMPTY),
          ByteString.copyFromUtf8("<some data d>")
        ),
        resultOf(
          queryMetadataOf(queryIdOf(5), ByteString.EMPTY),
          ByteString.copyFromUtf8("<some data e>")
        )
      )
    val decryptQueriesRequest =
      DecryptQueriesRequest.newBuilder()
        .addAllEncryptedQueryResults(queriedData.map { it.toByteString() })
        .build()
    val decryptedQueries = privateMembershipCryptor.decryptQueryResults(decryptQueriesRequest)
    assertThat(decryptedQueries.getDecryptedQueryResultsList())
      .containsExactly(
        ByteString.copyFromUtf8("<some data a>"),
        ByteString.copyFromUtf8("<some data b>"),
        ByteString.copyFromUtf8("<some data c>"),
        ByteString.copyFromUtf8("<some data d>"),
        ByteString.copyFromUtf8("<some data e>")
      )
  }
}

private fun queryBundleOf(shard: Int, queries: List<Pair<Int, Int>>): QueryBundle {
  return PlaintextQueryEvaluatorTestHelper.makeQueryBundle(
    shardIdOf(shard),
    queries.map { queryIdOf(it.first) to bucketIdOf(it.second) }
  )
}
