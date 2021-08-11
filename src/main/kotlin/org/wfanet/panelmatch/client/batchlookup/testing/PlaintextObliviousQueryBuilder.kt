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
import com.google.protobuf.ListValue
import org.wfanet.panelmatch.client.batchlookup.BucketId
import org.wfanet.panelmatch.client.batchlookup.DecryptQueriesRequest
import org.wfanet.panelmatch.client.batchlookup.DecryptQueriesResponse
import org.wfanet.panelmatch.client.batchlookup.EncryptQueriesRequest
import org.wfanet.panelmatch.client.batchlookup.EncryptQueriesResponse
import org.wfanet.panelmatch.client.batchlookup.GenerateKeysRequest
import org.wfanet.panelmatch.client.batchlookup.GenerateKeysResponse
import org.wfanet.panelmatch.client.batchlookup.ObliviousQueryBuilder
import org.wfanet.panelmatch.client.batchlookup.QueryBundle
import org.wfanet.panelmatch.client.batchlookup.QueryId
import org.wfanet.panelmatch.client.batchlookup.ShardId
import org.wfanet.panelmatch.client.batchlookup.queryBundleOf
import org.wfanet.panelmatch.client.batchlookup.queryMetadataOf

/**
 * Fake [QueryEvaluator] for testing purposes.
 *
 * Each [QueryBundle]'s payload is a serialized [ListValue] protocol buffer. Each element in the
 * list is a string -- the decimal string representation of a bucket to select.
 *
 * For example, a [ListValue] to select buckets 10 and 14 might be:
 *
 * values { string_value: "10" } values { string_value: "14" }
 *
 * No additional data is stored in the query metadata beyond the query id.
 */
object PlaintextObliviousQueryBuilder : ObliviousQueryBuilder {

  fun makeQueryBundle(shard: ShardId, queries: List<Pair<QueryId, BucketId>>): QueryBundle {
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
   */
  private fun splitConcatenatedPayloads(combinedPayloads: String): List<String> {
    return Regex("(<[^>]+>)")
      .findAll(combinedPayloads)
      .map { match -> match.groupValues[1] }
      .toList()
  }

  override fun generateKeys(request: GenerateKeysRequest): GenerateKeysResponse {
    return GenerateKeysResponse.getDefaultInstance()
  }

  /**
   * Creates a fake set of ciphertexts where each ciphertext is just the serialized query bundles
   * for each shard
   */
  override fun encryptQueries(request: EncryptQueriesRequest): EncryptQueriesResponse {
    val unencryptedQueries = request.getUnencryptedQueryList()
    return EncryptQueriesResponse.newBuilder()
      .addAllCiphertexts(
        unencryptedQueries.groupBy { it.shardId }.map {
          makeQueryBundle(shard = it.key, queries = it.value.map { Pair(it.queryId, it.bucketId) })
            .toByteString()
        }
      )
      .build()
  }

  /** Simple plaintext decrypter that splits up data marked by <...> */
  override fun decryptQueries(request: DecryptQueriesRequest): DecryptQueriesResponse {
    val encryptedQueryResults = request.getEncryptedQueryResultsList()
    return DecryptQueriesResponse.newBuilder()
      .addAllDecryptedQueryResults(
        encryptedQueryResults
          .flatMap { data -> splitConcatenatedPayloads(data.toStringUtf8()) }
          .map { it -> ByteString.copyFromUtf8(it) }
      )
      .build()
  }
}
