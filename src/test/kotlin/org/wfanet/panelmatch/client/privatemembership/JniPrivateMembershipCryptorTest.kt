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
import com.google.privatemembership.batch.Shared.Parameters.CryptoParameters
import com.google.privatemembership.batch.Shared.Parameters.ShardParameters
import com.google.privatemembership.batch.parameters as clientParameters
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(kotlin.ExperimentalUnsignedTypes::class)
@RunWith(JUnit4::class)
class JniPrivateMembershipCryptorTest() {
  val privateMembershipCryptor =
    JniPrivateMembershipCryptor(
      clientParameters =
        clientParameters {
          this.shardParameters =
            ShardParameters.newBuilder()
              .apply {
                numberOfShards = 200
                numberOfBucketsPerShard = 2000
              }
              .build()
          this.cryptoParameters =
            CryptoParameters.newBuilder()
              .apply {
                logDegree = 12
                logT = 1
                variance = 8
                levelsOfRecursion = 2
                logCompressionFactor = 4
                logDecompositionModulus = 10
              }
              .addRequestModulus(18446744073708380161UL.toLong())
              .addRequestModulus(137438953471UL.toLong())
              .addResponseModulus(2056193UL.toLong())
              .build()
        }
    )

  @Test
  fun `encryptQueries with multiple shards`() {
    val encryptQueriesRequest = encryptQueriesRequest {
      unencryptedQuery +=
        listOf(
          unencryptedQueryOf(100, 1, 1),
          unencryptedQueryOf(100, 2, 2),
          unencryptedQueryOf(101, 3, 1),
          unencryptedQueryOf(101, 4, 5)
        )
    }
    val encryptedQueries = privateMembershipCryptor.encryptQueries(encryptQueriesRequest)
    assertThat(encryptedQueries.encryptedQueryList)
      .containsExactly(
        encryptedQueryOf(100, 1),
        encryptedQueryOf(100, 2),
        encryptedQueryOf(101, 3),
        encryptedQueryOf(101, 4)
      )
    // TODO need to validate the ciphertexts in some way as well
  }

  @Test
  fun `Decrypt Encrypted Result`() {
    // TODO
  }
}
