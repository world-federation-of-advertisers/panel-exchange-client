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
import com.google.protobuf.kotlin.toByteStringUtf8
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.panelmatch.client.common.CompressedEvents
import org.wfanet.panelmatch.client.common.buildAsPCollectionView
import org.wfanet.panelmatch.client.common.queryIdOf
import org.wfanet.panelmatch.client.eventpreprocessing.compressByKey
import org.wfanet.panelmatch.client.privatemembership.DecryptEventDataRequest.EncryptedEventDataSet
import org.wfanet.panelmatch.client.privatemembership.DecryptedEventDataSet
import org.wfanet.panelmatch.client.privatemembership.EncryptedQueryResult
import org.wfanet.panelmatch.client.privatemembership.KeyedDecryptedEventDataSet
import org.wfanet.panelmatch.client.privatemembership.Plaintext
import org.wfanet.panelmatch.client.privatemembership.PrivateMembershipCryptor
import org.wfanet.panelmatch.client.privatemembership.QueryId
import org.wfanet.panelmatch.client.privatemembership.QueryIdAndJoinKeys
import org.wfanet.panelmatch.client.privatemembership.QueryResultsDecryptor
import org.wfanet.panelmatch.client.privatemembership.decryptQueryResults
import org.wfanet.panelmatch.client.privatemembership.decryptedEventDataSet
import org.wfanet.panelmatch.client.privatemembership.plaintext
import org.wfanet.panelmatch.common.beam.groupByKey
import org.wfanet.panelmatch.common.beam.keyBy
import org.wfanet.panelmatch.common.beam.kvOf
import org.wfanet.panelmatch.common.beam.map
import org.wfanet.panelmatch.common.beam.parDo
import org.wfanet.panelmatch.common.beam.strictOneToOneJoin
import org.wfanet.panelmatch.common.beam.testing.BeamTestBase
import org.wfanet.panelmatch.common.beam.testing.assertThat
import org.wfanet.panelmatch.common.compression.CompressorFactory
import org.wfanet.panelmatch.common.compression.DictionaryBuilder
import org.wfanet.panelmatch.common.crypto.AsymmetricKeys

private val PLAINTEXTS: List<Pair<Int, List<Plaintext>>> =
  listOf(
    1 to listOf(plaintextOf("<some long data a>"), plaintextOf("<some long data b>")),
    2 to listOf(plaintextOf("<some long data c>"), plaintextOf("<some long data d>")),
    3 to listOf(plaintextOf("<some long data e>"))
  )
private val QUERY_ID_AND_JOIN_KEYS =
  listOf(
    queryIdAndJoinKeysOf(1, "some lookup key 1", "some hashed joinkey 1"),
    queryIdAndJoinKeysOf(2, "some lookup key 2", "some hashed joinkey 2"),
    queryIdAndJoinKeysOf(3, "some lookup key 3", "some hashed joinkey 3")
  )
private val HKDF_PEPPER = "some-pepper".toByteStringUtf8()

@RunWith(JUnit4::class)
abstract class AbstractDecryptQueryResultsTest : BeamTestBase() {
  abstract val queryResultsDecryptor: QueryResultsDecryptor
  abstract val privateMembershipCryptor: PrivateMembershipCryptor
  abstract val privateMembershipCryptorHelper: PrivateMembershipCryptorHelper
  abstract val privateMembershipSerializedParameters: ByteString
  abstract val dictionaryBuilder: DictionaryBuilder
  abstract val compressorFactory: CompressorFactory

  private fun runWorkflow(
    queryResultsDecryptor: QueryResultsDecryptor
  ): PCollection<KeyedDecryptedEventDataSet> {
    val keys = privateMembershipCryptor.generateKeys()
    val plaintextCollection: PCollection<DecryptedEventDataSet> =
      pcollectionOf(
        "Create plaintext data",
        PLAINTEXTS.map {
          decryptedEventDataSet {
            queryId = queryIdOf(it.first)
            decryptedEventData += it.second
          }
        }
      )
    val compressedEvents = makeCompressedEvents(plaintextCollection)
    val queryIdAndJoinKeys: PCollection<QueryIdAndJoinKeys> =
      pcollectionOf("Create joinkey data", QUERY_ID_AND_JOIN_KEYS)
    val encryptedResults =
      makeEncryptedResults(
        privateMembershipCryptorHelper,
        keys,
        queryIdAndJoinKeys,
        compressedEvents.events
      )

    return decryptQueryResults(
      encryptedQueryResults = encryptedResults,
      queryIdAndJoinKeys = queryIdAndJoinKeys,
      compressor = compressorFactory.buildAsPCollectionView(compressedEvents.dictionary),
      privateMembershipKeys = pcollectionViewOf("Keys View", keys),
      serializedParameters = privateMembershipSerializedParameters,
      queryResultsDecryptor = queryResultsDecryptor,
      hkdfPepper = HKDF_PEPPER
    )
  }

  @Test
  fun `Decrypt simple set of results`() {
    val decryptedResults = runWorkflow(queryResultsDecryptor)
    assertThat(decryptedResults).satisfies {
      assertThat(
          it
            .map { dataset ->
              dataset.decryptedEventDataList.map { plaintext ->
                Pair(dataset.hashedJoinKey.key.toStringUtf8(), plaintext)
              }
            }
            .flatten()
        )
        .containsExactly(
          "some hashed joinkey 1" to plaintextOf("<some long data a>"),
          "some hashed joinkey 1" to plaintextOf("<some long data b>"),
          "some hashed joinkey 2" to plaintextOf("<some long data c>"),
          "some hashed joinkey 2" to plaintextOf("<some long data d>"),
          "some hashed joinkey 3" to plaintextOf("<some long data e>"),
        )
      null
    }
  }

  private fun makeCompressedEvents(
    decryptedEventDataSets: PCollection<DecryptedEventDataSet>,
  ): CompressedEvents {
    val events: PCollection<KV<ByteString, ByteString>> =
      decryptedEventDataSets.parDo { dataSet: DecryptedEventDataSet ->
        for (decryptedEventData in dataSet.decryptedEventDataList) {
          yield(kvOf(dataSet.queryId.toByteString(), requireNotNull(decryptedEventData.payload)))
        }
      }
    return dictionaryBuilder.compressByKey(events)
  }

  private fun makeEncryptedResults(
    privateMembershipCryptorHelper: PrivateMembershipCryptorHelper,
    keys: AsymmetricKeys,
    queryIdAndJoinKeys: PCollection<QueryIdAndJoinKeys>,
    events: PCollection<KV<ByteString, ByteString>>
  ): PCollection<EncryptedQueryResult> {

    val compressedPlaintexts: PCollection<KV<QueryId, DecryptedEventDataSet>> =
      events
        .map { kvOf(QueryId.parseFrom(it.key), it.value) }
        .groupByKey("Group By QueryId")
        .map {
          decryptedEventDataSet {
            decryptedEventData += it.value.map { plaintext { payload = it } }
            queryId = it.key
          }
        }
        .keyBy { it.queryId }
    val keyedQueryIdAndJoinKeys = queryIdAndJoinKeys.keyBy { it.queryId }
    val encryptedEventDataSet: PCollection<EncryptedEventDataSet> =
      compressedPlaintexts.strictOneToOneJoin(keyedQueryIdAndJoinKeys).map {
        privateMembershipCryptorHelper.makeEncryptedEventDataSet(
          it.key,
          it.key.queryId to it.value.lookupKey
        )
      }

    return encryptedEventDataSet.map {
      privateMembershipCryptorHelper.makeEncryptedQueryResult(keys, it)
    }
  }
}
