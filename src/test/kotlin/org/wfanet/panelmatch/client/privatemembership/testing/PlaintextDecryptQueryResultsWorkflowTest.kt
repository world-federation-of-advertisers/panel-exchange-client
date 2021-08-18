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
import org.apache.beam.sdk.values.PCollection
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.panelmatch.client.privatemembership.Result
import org.wfanet.panelmatch.client.privatemembership.queryIdOf
import org.wfanet.panelmatch.client.privatemembership.queryMetadataOf
import org.wfanet.panelmatch.client.privatemembership.resultOf

@RunWith(JUnit4::class)
class PlaintextDecryptQueryResultsWorkflowTest : AbstractDecryptQueryResultsWorkflowTest() {
  override val privateMembershipCryptor = PlaintextPrivateMembershipCryptor
  override val privateMembershipCryptorHelper = PlaintextPrivateMembershipCryptorHelper
  override val encryptedResults by lazy {
    encryptedResultOf(
      resultOf(
        queryMetadataOf(queryIdOf(1), ByteString.EMPTY),
        ByteString.copyFromUtf8("<some data a>")
      ),
      resultOf(
        queryMetadataOf(queryIdOf(2), ByteString.EMPTY),
        ByteString.copyFromUtf8("<some data b>")
      ),
      resultOf(
        queryMetadataOf(queryIdOf(3), ByteString.EMPTY),
        ByteString.copyFromUtf8("<some data c>")
      ),
      resultOf(
        queryMetadataOf(queryIdOf(4), ByteString.EMPTY),
        ByteString.copyFromUtf8("<some data d>")
      ),
      resultOf(
        queryMetadataOf(queryIdOf(5), ByteString.EMPTY),
        ByteString.copyFromUtf8("<some data e>")
      )
    )
  }
  private fun encryptedResultOf(vararg entries: Result): PCollection<ByteString> {
    return pcollectionOf(
      "Create encryptedResults",
      *entries.map { it.toByteString() }.toTypedArray()
    )
  }
}
