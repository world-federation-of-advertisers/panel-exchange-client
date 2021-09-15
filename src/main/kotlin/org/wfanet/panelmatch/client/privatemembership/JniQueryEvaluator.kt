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

import com.google.privatemembership.batch.Shared.EncryptedQueries
import com.google.privatemembership.batch.Shared.EncryptedQueryResult
import com.google.privatemembership.batch.Shared.Parameters
import com.google.privatemembership.batch.Shared.PublicKey
import com.google.privatemembership.batch.Shared.QueryMetadata
import com.google.privatemembership.batch.encryptedQueryResult
import com.google.privatemembership.batch.server.ApplyQueriesRequestKt.rawDatabase
import com.google.privatemembership.batch.server.RawDatabaseShardKt.bucket
import com.google.privatemembership.batch.server.Server.RawDatabaseShard
import com.google.privatemembership.batch.server.SumCiphertextsRequestKt.summation
import com.google.privatemembership.batch.server.applyQueriesRequest
import com.google.privatemembership.batch.server.finalizeResultsRequest
import com.google.privatemembership.batch.server.rawDatabaseShard
import com.google.privatemembership.batch.server.sumCiphertextsRequest
import com.google.protobuf.ByteString
import rlwe.Serialization.SerializedSymmetricRlweCiphertext

/** [QueryEvaluator] that calls into C++ via JNI. */
class JniQueryEvaluator(private val parameters: QueryEvaluatorParameters) : QueryEvaluator {
  private val privateMembershipParameters: Parameters by lazy {
    Parameters.parseFrom(parameters.serializedPrivateMembershipParameters)
  }

  private val privateMembershipPublicKey: PublicKey by lazy {
    PublicKey.parseFrom(parameters.serializedPublicKey)
  }

  override fun executeQueries(
    shards: List<DatabaseShard>,
    queryBundles: List<QueryBundle>
  ): List<Result> {
    val request = applyQueriesRequest {
      parameters = privateMembershipParameters
      publicKey = privateMembershipPublicKey
      finalizeResults = false
      queries += queryBundles.map(QueryBundle::encryptedQueries)
      rawDatabase =
        rawDatabase {
          this.shards += shards.map(DatabaseShard::toPrivateMembershipRawDatabaseShard)
        }
    }

    val response = JniPrivateMembership.applyQueries(request)

    return response.queryResultsList.map { encryptedQueryResult ->
      resultOf(
        queryIdOf(encryptedQueryResult.queryMetadata.queryId),
        encryptedQueryResult.toByteString()
      )
    }
  }

  override fun combineResults(results: Sequence<Result>): Result {
    val sums = mutableListOf<MutableList<SerializedSymmetricRlweCiphertext>>()
    var firstQueryMetadata: QueryMetadata? = null
    val queryIds = mutableSetOf<Int>()

    for (result in results) {
      val encryptedQueryResult =
        EncryptedQueryResult.parseFrom(result.serializedEncryptedQueryResult)
      for ((i, ciphertext) in encryptedQueryResult.ciphertextsList.withIndex()) {
        if (i == sums.size) sums += mutableListOf<SerializedSymmetricRlweCiphertext>()
        sums[i].add(ciphertext)
      }

      if (firstQueryMetadata == null) firstQueryMetadata = encryptedQueryResult.queryMetadata

      check(result.queryId.id == encryptedQueryResult.queryMetadata.queryId)
      queryIds.add(result.queryId.id)
    }

    require(queryIds.size == 1) { "Mismatching query ids" }
    requireNotNull(firstQueryMetadata) { "Cannot combine zero results" }

    if (sums.isEmpty()) {
      return resultOf(queryIdOf(firstQueryMetadata.queryId), ByteString.EMPTY)
    }

    val request = sumCiphertextsRequest {
      parameters = privateMembershipParameters
      summations += sums.map { summation { ciphertexts += it } }
    }

    val response = JniPrivateMembership.sumCiphertexts(request)

    val encryptedQueryResult = encryptedQueryResult {
      queryMetadata = firstQueryMetadata
      ciphertexts += response.ciphertextsList
    }

    return resultOf(queryIdOf(firstQueryMetadata.queryId), encryptedQueryResult.toByteString())
  }

  override fun finalizeResults(results: Sequence<Result>): Sequence<Result> {
    val request = finalizeResultsRequest {
      parameters = privateMembershipParameters
      for (result in results) {
        encryptedResults += result.encryptedQueryResult
      }
    }

    val response = JniPrivateMembership.finalizeResults(request)

    return response.finalizedResultsList.asSequence().map {
      resultOf(queryIdOf(it.queryMetadata.queryId), it.toByteString())
    }
  }
}

private val QueryBundle.encryptedQueries: EncryptedQueries
  get() = EncryptedQueries.parseFrom(serializedEncryptedQueries)

private val Result.encryptedQueryResult: EncryptedQueryResult
  get() = EncryptedQueryResult.parseFrom(serializedEncryptedQueryResult)

private fun DatabaseShard.toPrivateMembershipRawDatabaseShard(): RawDatabaseShard =
    rawDatabaseShard {
  shardIndex = shardId.id
  buckets +=
    bucketsList.map {
      bucket {
        bucketId = it.bucketId.id
        bucketContents = it.payload
      }
    }
}
