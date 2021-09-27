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
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.panelmatch.client.privatemembership.DecryptEventDataRequest.EncryptedEventDataSet
import org.wfanet.panelmatch.client.privatemembership.DecryptQueryResultsWorkflow
import org.wfanet.panelmatch.client.privatemembership.DecryptQueryResultsWorkflow.Parameters
import org.wfanet.panelmatch.client.privatemembership.DecryptedEventDataSet
import org.wfanet.panelmatch.client.privatemembership.EncryptedQueryResult
import org.wfanet.panelmatch.client.privatemembership.JoinKey
import org.wfanet.panelmatch.client.privatemembership.Plaintext
import org.wfanet.panelmatch.client.privatemembership.PrivateMembershipCryptor
import org.wfanet.panelmatch.client.privatemembership.PrivateMembershipKeys
import org.wfanet.panelmatch.client.privatemembership.QueryId
import org.wfanet.panelmatch.client.privatemembership.QueryResultsDecryptor
import org.wfanet.panelmatch.client.privatemembership.joinKeyOf
import org.wfanet.panelmatch.client.privatemembership.plaintextOf
import org.wfanet.panelmatch.client.privatemembership.queryIdOf
import org.wfanet.panelmatch.common.beam.kvOf
import org.wfanet.panelmatch.common.beam.testing.BeamTestBase
import org.wfanet.panelmatch.common.beam.testing.assertThat
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

@RunWith(JUnit4::class)
abstract class AbstractDecryptQueryResultsWorkflowTest : BeamTestBase() {
  abstract val queryResultsDecryptor: QueryResultsDecryptor
  abstract val privateMembershipCryptor: PrivateMembershipCryptor
  abstract val privateMembershipCryptorHelper: PrivateMembershipCryptorHelper
  abstract val privateMembershipSerializedParameters: ByteString

  private fun runWorkflow(
    queryResultsDecryptor: QueryResultsDecryptor,
    parameters: Parameters
  ): PCollection<DecryptedEventDataSet> {
    val encryptedEventDataSet: List<EncryptedEventDataSet> =
      privateMembershipCryptorHelper.makeEncryptedEventDataSet(PLAINTEXTS, JOINKEYS)
    val keys =
      PrivateMembershipKeys(
        serializedPrivateKey = parameters.serializedPrivateKey,
        serializedPublicKey = parameters.serializedPublicKey
      )
    val encryptedResults: PCollection<EncryptedQueryResult> =
      encryptedResultOf(
        encryptedEventDataSet.map {
          privateMembershipCryptorHelper.makeEncryptedQueryResult(keys, it)
        }
      )
    val joinkeyCollection = joinkeyCollectionOf(JOINKEYS)
    return DecryptQueryResultsWorkflow(
        parameters = parameters,
        queryResultsDecryptor = queryResultsDecryptor,
        hkdfPepper = HKDF_PEPPER,
      )
      .batchDecryptQueryResults(
        encryptedQueryResults = encryptedResults,
        queryIdToJoinKey = joinkeyCollection,
      )
  }

  @Test
  fun `Decrypt simple set of results`() {
    val keys = privateMembershipCryptor.generateKeys()
    val parameters =
      Parameters(
        serializedParameters = privateMembershipSerializedParameters,
        serializedPrivateKey = keys.serializedPrivateKey,
        serializedPublicKey = keys.serializedPublicKey
      )
    val decryptedResults = runWorkflow(queryResultsDecryptor, parameters)
    assertThat(decryptedResults).satisfies {
      assertThat(
          it
            .map { dataset ->
              dataset.decryptedEventDataList.map { plaintext -> Pair(dataset.queryId, plaintext) }
            }
            .flatten()
        )
        .containsExactly(
          Pair(queryIdOf(1), plaintextOf("<some long data a>")),
          Pair(queryIdOf(1), plaintextOf("<some long data b>")),
          Pair(queryIdOf(2), plaintextOf("<some long data c>")),
          Pair(queryIdOf(2), plaintextOf("<some long data d>")),
          Pair(queryIdOf(3), plaintextOf("<some long data e>")),
        )
      null
    }
  }

  private fun encryptedResultOf(
    entries: List<EncryptedQueryResult>
  ): PCollection<EncryptedQueryResult> {
    return pcollectionOf("Create encryptedResults", *entries.map { it }.toTypedArray())
  }

  private fun joinkeyCollectionOf(
    entries: List<Pair<Int, String>>
  ): PCollection<KV<QueryId, JoinKey>> {
    return pcollectionOf(
      "Create encryptedResults",
      *entries.map { kvOf(queryIdOf(it.first), joinKeyOf(it.second.toByteString())) }.toTypedArray()
    )
  }
}
