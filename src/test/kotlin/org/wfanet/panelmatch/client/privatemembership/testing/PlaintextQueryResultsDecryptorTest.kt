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
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.panelmatch.client.privatemembership.DecryptEventDataRequest.EncryptedEventDataSet
import org.wfanet.panelmatch.client.privatemembership.Plaintext
import org.wfanet.panelmatch.client.privatemembership.decryptQueryResultsRequest
import org.wfanet.panelmatch.client.privatemembership.joinKeyOf
import org.wfanet.panelmatch.client.privatemembership.plaintextOf
import org.wfanet.panelmatch.client.privatemembership.queryIdOf
import org.wfanet.panelmatch.common.toByteString

private val PLAINTEXTS: List<Pair<Int, List<Plaintext>>> =
  listOf(
    Pair(1, listOf(plaintextOf("<some long data a>"), plaintextOf("<some long data b>"))),
    Pair(2, listOf(plaintextOf("<some long data c>"), plaintextOf("<some long data d>"))),
    Pair(3, listOf(plaintextOf("<some long data e>")))
  )
private val JOINKEYS =
  listOf(Pair(1, "some joinkey 1"), Pair(2, "some joinkey 1"), Pair(3, "some joinkey 1"))
private val HKDF_PEPPER = "some-pepper".toByteString()
private val SERIALIZED_PARAMETERS = "some-serialized-parameters".toByteString()

@RunWith(JUnit4::class)
class PlaintextQueryResultsDecryptorTest {
  val queryResultsDecryptor = PlaintextQueryResultsDecryptor()
  val privateMembershipCryptor = PlaintextPrivateMembershipCryptor(SERIALIZED_PARAMETERS)
  val privateMembershipCryptorHelper = PlaintextPrivateMembershipCryptorHelper

  @Test
  fun `decryptQueries`() {
    val keys = privateMembershipCryptor.generateKeys()

    val encryptedEventData: List<EncryptedEventDataSet> =
      privateMembershipCryptorHelper.makeEncryptedEventDataSet(PLAINTEXTS, JOINKEYS)
    val encryptedQueryResults =
      encryptedEventData.map { privateMembershipCryptorHelper.makeEncryptedQueryResult(keys, it) }

    val decryptedQueries =
      encryptedQueryResults
        .zip(JOINKEYS)
        .map { (encryptedQueryResult, joinkeyList) ->
          val request = decryptQueryResultsRequest {
            serializedParameters = SERIALIZED_PARAMETERS
            serializedPublicKey = keys.serializedPublicKey
            serializedPrivateKey = keys.serializedPrivateKey
            singleBlindedJoinkey = joinKeyOf(joinkeyList.second.toByteString())
            this.encryptedQueryResults += encryptedQueryResult
            hkdfPepper = HKDF_PEPPER
          }
          queryResultsDecryptor.decryptQueryResults(request).eventDataSetsList.map { eventSet ->
            eventSet.decryptedEventDataList.map { Pair(eventSet.queryId, it) }
          }
        }
        .flatten()
        .flatten()
    assertThat(decryptedQueries)
      .containsExactly(
        Pair(queryIdOf(1), plaintextOf("<some long data a>")),
        Pair(queryIdOf(1), plaintextOf("<some long data b>")),
        Pair(queryIdOf(2), plaintextOf("<some long data c>")),
        Pair(queryIdOf(2), plaintextOf("<some long data d>")),
        Pair(queryIdOf(3), plaintextOf("<some long data e>")),
      )
  }
}
