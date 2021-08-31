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

import com.google.privatemembership.batch.Shared.EncryptedQueryResult as ClientEncryptedQueryResult
import com.google.privatemembership.batch.Shared.Parameters as ClientParameters
import com.google.privatemembership.batch.Shared.PublicKey as ClientPublicKey
import com.google.privatemembership.batch.client.Client.DecryptQueriesResponse as ClientDecryptQueriesResponse
import com.google.privatemembership.batch.client.Client.EncryptQueriesResponse as ClientEncryptQueriesResponse
import com.google.privatemembership.batch.client.Client.GenerateKeysResponse as ClientGenerateKeysResponse
import com.google.privatemembership.batch.client.Client.PrivateKey as ClientPrivateKey
import com.google.privatemembership.batch.client.decryptQueriesRequest as clientDecryptQueriesRequest
import com.google.privatemembership.batch.client.encryptQueriesRequest as clientEncryptQueriesRequest
import com.google.privatemembership.batch.client.generateKeysRequest as clientGenerateKeysRequest
import com.google.privatemembership.batch.client.plaintextQuery as clientPlaintextQuery
import com.google.privatemembership.batch.encryptedQueryResult as clientEncryptedQueryResult
import com.google.privatemembership.batch.queryMetadata as clientQueryMetadata
import java.nio.file.Paths
import org.wfanet.panelmatch.common.loadLibrary
import org.wfanet.panelmatch.common.wrapJniException
import rlwe.Serialization.SerializedSymmetricRlweCiphertext

/**
 * A [SymmetricPrivateMembershipCryptor] implementation using the JNI
 * [SymmetricPrivateMembershipWrapper]. Keys should have been generated prior to this step using an
 * implementation of [PrivateMembershipWrapper].
 * */
class JniSymmetricPrivateMembershipCryptor : SymmetricPrivateMembershipCryptor {

  override fun decryptQueryResults(request: SymmetricDecryptQueryResultsRequest): SymmetricDecryptQueryResultsResponse {
    return wrapJniException {
      SymmetricDecryptQueryResultsResponse.parseFrom(
        SymmetricPrivateMembershipWrapper.symmetricDecryptQueryResultsWrapper(
          request.toByteArray()
        )
      )
    }
  }

  companion object {
    private val SWIG_PATH =
      "panel_exchange_client/src/main/swig/wfanet/panelmatch/client/privatemembership/symmetricprivatemembershipwrapper"
    init {
      loadLibrary(name = "symmetric_private_membership_wrapper", directoryPath = Paths.get(SWIG_PATH))
    }
  }
}
