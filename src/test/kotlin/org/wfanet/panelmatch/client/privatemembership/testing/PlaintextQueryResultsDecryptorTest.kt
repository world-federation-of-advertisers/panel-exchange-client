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
import com.google.protobuf.kotlin.toByteStringUtf8
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.panelmatch.client.common.queryIdOf
import org.wfanet.panelmatch.client.privatemembership.DecryptEventDataRequest.EncryptedEventDataSet
import org.wfanet.panelmatch.client.privatemembership.decryptQueryResultsRequest
import org.wfanet.panelmatch.client.privatemembership.decryptedEventDataSet

private data class QueryAndPlaintexts(
  val queryId: Int,
  val joinKey: String,
  val plaintexts: List<String>
)

private val QUERIES_AND_PLAINTEXTS: List<QueryAndPlaintexts> =
  listOf(
    QueryAndPlaintexts(1, "some-joinkey-1", listOf("<some-data-a>", "<some-data-b>")),
    QueryAndPlaintexts(2, "some-joinkey-2", listOf("<some-data-c>", "<some-data-d>")),
    QueryAndPlaintexts(3, "some-joinkey-3", listOf("<some-data-e>")),
  )

private val HKDF_PEPPER = "some-pepper".toByteStringUtf8()
private val SERIALIZED_PARAMETERS = "some-serialized-parameters".toByteStringUtf8()

@RunWith(JUnit4::class)
class PlaintextQueryResultsDecryptorTest {
  private val queryResultsDecryptor = PlaintextQueryResultsDecryptor()
  private val privateMembershipCryptor = PlaintextPrivateMembershipCryptor()
  private val privateMembershipCryptorHelper = PlaintextPrivateMembershipCryptorHelper()

  @Test
  fun decryptQueries() {
    val keys = privateMembershipCryptor.generateKeys()

    val encryptedEventData: List<EncryptedEventDataSet> =
      QUERIES_AND_PLAINTEXTS.map {
        privateMembershipCryptorHelper.makeEncryptedEventDataSet(
          decryptedEventDataSet {
            queryId = queryIdOf(it.queryId)
            decryptedEventData += it.plaintexts.map(String::toByteStringUtf8)
          },
          queryIdOf(it.queryId) to joinKeyOf(it.joinKey)
        )
      }
    val encryptedQueryResults =
      encryptedEventData.map { privateMembershipCryptorHelper.makeEncryptedQueryResult(keys, it) }

    val decryptedQueries =
      encryptedQueryResults
        .map { encryptedQueryResult ->
          val joinKey =
            QUERIES_AND_PLAINTEXTS.single { it.queryId == encryptedQueryResult.queryId.id }.joinKey
          val request = decryptQueryResultsRequest {
            serializedParameters = SERIALIZED_PARAMETERS
            serializedPublicKey = keys.serializedPublicKey
            serializedPrivateKey = keys.serializedPrivateKey
            lookupKey = joinKeyOf(joinKey)
            this.encryptedQueryResults += encryptedQueryResult
            hkdfPepper = HKDF_PEPPER
          }
          queryResultsDecryptor.decryptQueryResults(request).eventDataSetsList.map { eventSet ->
            eventSet.decryptedEventDataList.map { eventSet.queryId to it.toStringUtf8() }
          }
        }
        .flatten()
        .flatten()
    assertThat(decryptedQueries)
      .containsExactly(
        queryIdOf(1) to "<some-data-a>",
        queryIdOf(1) to "<some-data-b>",
        queryIdOf(2) to "<some-data-c>",
        queryIdOf(2) to "<some-data-d>",
        queryIdOf(3) to "<some-data-e>",
      )
  }
}
