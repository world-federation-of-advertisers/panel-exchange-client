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
import com.google.protobuf.listValue
import com.google.protobuf.value
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.wfanet.panelmatch.client.privatemembership.BucketId
import org.wfanet.panelmatch.client.privatemembership.DecryptEventDataRequest.EncryptedEventDataSet
import org.wfanet.panelmatch.client.privatemembership.DecryptEventDataRequestKt.encryptedEventDataSet
import org.wfanet.panelmatch.client.privatemembership.DecryptedQueryResult
import org.wfanet.panelmatch.client.privatemembership.EncryptedQueryBundle
import org.wfanet.panelmatch.client.privatemembership.EncryptedQueryResult
import org.wfanet.panelmatch.client.privatemembership.Plaintext
import org.wfanet.panelmatch.client.privatemembership.PrivateMembershipKeys
import org.wfanet.panelmatch.client.privatemembership.QueryId
import org.wfanet.panelmatch.client.privatemembership.ShardId
import org.wfanet.panelmatch.client.privatemembership.decryptedQueryResult
import org.wfanet.panelmatch.client.privatemembership.encryptedEventData
import org.wfanet.panelmatch.client.privatemembership.queryBundleOf
import org.wfanet.panelmatch.client.privatemembership.queryIdOf
import org.wfanet.panelmatch.client.privatemembership.resultOf
import org.wfanet.panelmatch.common.beam.kvOf
import org.wfanet.panelmatch.common.beam.parDo
import org.wfanet.panelmatch.common.beam.values
import org.wfanet.panelmatch.common.crypto.SymmetricCryptor
import org.wfanet.panelmatch.common.crypto.testing.ConcatSymmetricCryptor
import org.wfanet.panelmatch.common.toByteString

object PlaintextPrivateMembershipCryptorHelper : PrivateMembershipCryptorHelper {

  private val symmetricCryptor: SymmetricCryptor = ConcatSymmetricCryptor()

  override fun makeEncryptedQuery(
    shard: ShardId,
    queries: List<Pair<QueryId, BucketId>>
  ): EncryptedQueryBundle {
    return queryBundleOf(
      shard,
      queries.map { it.first },
      listValue {
          for (query in queries) {
            values += value { stringValue = query.second.id.toString() }
          }
        }
        .toByteString()
    )
  }

  private fun decodeEncryptedQuery(queryBundle: EncryptedQueryBundle): List<ShardedQuery> {
    val queryIdsList = queryBundle.queryIdsList
    val bucketValuesList =
      ListValue.parseFrom(queryBundle.serializedEncryptedQueries).valuesList.map {
        it.stringValue.toInt()
      }
    return queryIdsList.zip(bucketValuesList) { queryId: QueryId, bucketValue: Int ->
      ShardedQuery(requireNotNull(queryBundle.shardId).id, queryId.id, bucketValue)
    }
  }

  override fun makeEncryptedQueryResult(
    keys: PrivateMembershipKeys,
    encryptedEventDataSet: EncryptedEventDataSet
  ): EncryptedQueryResult {
    return resultOf(
      encryptedEventDataSet.queryId,
      encryptedEventDataSet.encryptedEventData.toByteString()
    )
  }

  override fun decodeEncryptedQueryResult(result: EncryptedQueryResult): DecryptedQueryResult {
    return decryptedQueryResult {
      queryId = result.queryId
      queryResult = result.serializedEncryptedQueryResult
    }
  }

  override fun makeEncryptedEventDataSet(
    plaintexts: List<Pair<Int, List<Plaintext>>>,
    joinkeys: List<Pair<Int, String>>
  ): List<EncryptedEventDataSet> {
    return plaintexts.zip(joinkeys).map { (plaintextList, joinkeyList) ->
      encryptedEventDataSet {
        queryId = queryIdOf(plaintextList.first)
        encryptedEventData =
          encryptedEventData {
            ciphertexts +=
              plaintextList.second.map { plaintext ->
                symmetricCryptor.encrypt(joinkeyList.second.toByteString(), plaintext.payload)
              }
          }
      }
    }
  }

  override fun decodeEncryptedQuery(
    data: PCollection<KV<ShardId, ByteString>>
  ): PCollection<KV<QueryId, ShardedQuery>> {
    return data.values().parDo("Map to ShardedQuery") { encryptedQueriesData ->
      yieldAll(
        decodeEncryptedQuery(EncryptedQueryBundle.parseFrom(encryptedQueriesData)).map {
          kvOf(it.queryId, ShardedQuery(it.shardId.id, it.queryId.id, it.bucketId.id))
        }
      )
    }
  }
}
