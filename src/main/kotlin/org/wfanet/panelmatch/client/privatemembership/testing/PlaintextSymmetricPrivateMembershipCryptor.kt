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

import org.wfanet.panelmatch.client.privatemembership.PrivateMembershipCryptor
import org.wfanet.panelmatch.client.privatemembership.SymmetricDecryptQueryResultsRequest
import org.wfanet.panelmatch.client.privatemembership.SymmetricDecryptQueryResultsResponse
import org.wfanet.panelmatch.client.privatemembership.SymmetricPrivateMembershipCryptor
import org.wfanet.panelmatch.client.privatemembership.decryptQueryResultsRequest
import org.wfanet.panelmatch.client.privatemembership.decryptedEventData
import org.wfanet.panelmatch.client.privatemembership.symmetricDecryptQueryResultsResponse
import org.wfanet.panelmatch.common.crypto.SymmetricCryptor
import org.wfanet.panelmatch.common.crypto.testing.ConcatSymmetricCryptor

class PlaintextSymmetricPrivateMembershipCryptor(
  private val privateMembershipCryptor: PrivateMembershipCryptor =
    PlaintextPrivateMembershipCryptor,
  private val symmetricCryptor: SymmetricCryptor = ConcatSymmetricCryptor(),
) : SymmetricPrivateMembershipCryptor {

  override fun decryptQueryResults(
    request: SymmetricDecryptQueryResultsRequest
  ): SymmetricDecryptQueryResultsResponse {
    val privateCryptorRequest = decryptQueryResultsRequest {
      serializedParameters = request.serializedParameters
      serializedPublicKey = request.serializedPublicKey
      serializedPrivateKey = request.serializedPrivateKey
      encryptedQueryResults += request.encryptedQueryResultsList
    }
    val privateCryptorResponse = privateMembershipCryptor.decryptQueryResults(privateCryptorRequest)
    return symmetricDecryptQueryResultsResponse {
      decryptedEventData +=
        privateCryptorResponse.decryptedQueryResultsList.map {
          decryptedEventData {
            queryId = it.queryId
            shardId = it.shardId
            plaintext = symmetricCryptor.decrypt(request.singleBlindedJoinkey.key, it.queryResult)
          }
        }
    }
  }
}
