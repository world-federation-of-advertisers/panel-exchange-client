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

import com.google.protobuf.ByteString
import com.google.protobuf.ListValue
import com.google.protobuf.value
import org.wfanet.panelmatch.client.privatemembership.BucketId
import org.wfanet.panelmatch.client.privatemembership.GenerateKeysRequest
import org.wfanet.panelmatch.client.privatemembership.GenerateKeysResponse
import org.wfanet.panelmatch.client.privatemembership.PrivateMembershipCryptor
import org.wfanet.panelmatch.client.privatemembership.PrivateMembershipEncryptRequest
import org.wfanet.panelmatch.client.privatemembership.PrivateMembershipEncryptResponse
import org.wfanet.panelmatch.client.privatemembership.QueryBundle
import org.wfanet.panelmatch.client.privatemembership.QueryId
import org.wfanet.panelmatch.client.privatemembership.ShardId
import org.wfanet.panelmatch.client.privatemembership.encryptedQuery
import org.wfanet.panelmatch.client.privatemembership.generateKeysResponse
import org.wfanet.panelmatch.client.privatemembership.privateMembershipEncryptResponse
import org.wfanet.panelmatch.client.privatemembership.queryBundleOf
import org.wfanet.panelmatch.client.privatemembership.queryMetadataOf
import org.wfanet.panelmatch.common.toByteString

/**
 * Fake [PlaintextPrivateMembershipCryptor] for testing purposes.
 *
 * Built to be compatible with the [PlaintextQueryEvaluator].
 */
object PlaintextPrivateMembershipCryptor : PrivateMembershipCryptor {

  private fun makeQueryBundle(shard: ShardId, queries: List<Pair<QueryId, BucketId>>): QueryBundle {
    return queryBundleOf(
      shard,
      queries.map { queryMetadataOf(it.first, ByteString.EMPTY) },
      ListValue.newBuilder()
        .apply {
          for (query in queries) {
            addValuesBuilder().stringValue = query.second.id.toString()
          }
        }
        .build()
        .toByteString()
    )
  }

  /**
   * Splits [combinedPayloads] into individual payloads.
   *
   * We assume that each individual payload's first and last characters are '<' and '>',
   * respectively.
   *
   * TODO: This is not currently used but to better mimic the RLWE with FHE we should re-introduce
   * ciphertext separators.
   */
  private fun splitConcatenatedPayloads(combinedPayloads: String): List<String> {
    return Regex("(<[^>]+>)")
      .findAll(combinedPayloads)
      .map { match -> match.groupValues[1] }
      .toList()
  }

  override fun generateKeys(request: GenerateKeysRequest): GenerateKeysResponse {
    return generateKeysResponse {
      serializedPublicKey = ByteString.EMPTY
      serializedPrivateKey = ByteString.EMPTY
    }
  }

  /**
   * Creates a fake set of ciphertexts where each ciphertext is just the serialized query bundles
   * for each shard
   */
  override fun encryptQueries(
    request: PrivateMembershipEncryptRequest
  ): PrivateMembershipEncryptResponse {
    val unencryptedQueries = request.unencryptedQueriesList
    return privateMembershipEncryptResponse {
      ciphertexts +=
        unencryptedQueries.groupBy { it.shardId }.map {
          makeQueryBundle(shard = it.key, queries = it.value.map { Pair(it.queryId, it.bucketId) })
            .toByteString()
        }
      this.encryptedQuery +=
        unencryptedQueries.map {
          encryptedQuery {
            shardId = it.shardId
            queryId = it.queryId
          }
        }
    }
  }
}
