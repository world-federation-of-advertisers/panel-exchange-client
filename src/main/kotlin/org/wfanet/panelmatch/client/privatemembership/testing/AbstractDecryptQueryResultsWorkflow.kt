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
import org.wfanet.panelmatch.client.privatemembership.ObliviousQueryParameters
import org.wfanet.panelmatch.client.privatemembership.DecryptQueryResultsWorkflow
import org.wfanet.panelmatch.client.privatemembership.DecryptQueryResultsWorkflow.Parameters
import org.wfanet.panelmatch.client.privatemembership.EncryptQueriesResponse
import org.wfanet.panelmatch.client.privatemembership.JoinKey
import org.wfanet.panelmatch.client.privatemembership.PanelistKey
import org.wfanet.panelmatch.client.privatemembership.PrivateMembershipCryptor
import org.wfanet.panelmatch.client.privatemembership.QueryId
import org.wfanet.panelmatch.client.privatemembership.joinKeyOf
import org.wfanet.panelmatch.client.privatemembership.panelistKeyOf
import org.wfanet.panelmatch.common.beam.join
import org.wfanet.panelmatch.common.beam.kvOf
import org.wfanet.panelmatch.common.beam.parDo
import org.wfanet.panelmatch.common.beam.map
import org.wfanet.panelmatch.common.beam.testing.BeamTestBase
import org.wfanet.panelmatch.common.beam.testing.assertThat
import org.wfanet.panelmatch.common.beam.values

@RunWith(JUnit4::class)
abstract class AbstractDecryptQueryResultsWorkflowTest : BeamTestBase() {
  private val encryptedResults by lazy {
    encryptedResultsOf("abc", "def", "hij", "klm", "nop", "qrs")
  }
  abstract val privateMembershipCryptor: PrivateMembershipCryptor
  abstract val privateMembershipCryptorHelper: PrivateMembershipCryptorHelper

  private fun runWorkflow(
    privateMembershipCryptor: PrivateMembershipCryptor,
    parameters: Parameters
  ): PCollection<ByteString> {
    return DecryptQueryResultsWorkflow(
      parameters = parameters,
      privateMembershipCryptor = privateMembershipCryptor
    )
      .batchDecryptQueryResults(encryptedResults)
  }

  @Test
  fun `Decrypt Results across two shards`() {
    val parameters = Parameters(obliviousQueryParameters = ObliviousQueryParameters.getDefaultInstance())
    val decryptedResults = runWorkflow(privateMembershipCryptor, parameters)
    assertThat(decryptedResults)
      .containsInAnyOrder(
        ByteString.copyFromUtf8("asdf")
      )
  }

  private fun encryptedResultsOf(
    vararg entries: String
  ): PCollection<ByteString> {
    return pcollectionOf(
      "Create encryptedResults",
      *entries
        .map { ByteString.copyFromUtf8(it) }
        .toTypedArray()
    )
  }
}
