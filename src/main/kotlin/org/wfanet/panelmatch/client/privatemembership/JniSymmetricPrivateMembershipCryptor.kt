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

import java.nio.file.Paths
import org.wfanet.panelmatch.common.loadLibrary
import org.wfanet.panelmatch.common.wrapJniException

/**
 * A [SymmetricPrivateMembershipCryptor] implementation using the JNI
 * [SymmetricPrivateMembershipWrapper]. Keys should have been generated prior to this step using an
 * implementation of [PrivateMembershipWrapper].
 */
class JniSymmetricPrivateMembershipCryptor : SymmetricPrivateMembershipCryptor {

  override fun decryptQueryResults(
    request: SymmetricDecryptQueryResultsRequest
  ): SymmetricDecryptQueryResultsResponse {
    return wrapJniException {
      SymmetricDecryptQueryResultsResponse.parseFrom(
        SymmetricPrivateMembershipWrapper.symmetricDecryptQueryResultsWrapper(request.toByteArray())
      )
    }
  }

  companion object {
    private val SWIG_PATH =
      "panel_exchange_client/src/main/swig/wfanet/panelmatch/client/privatemembership/symmetricprivatemembershipwrapper"
    init {
      loadLibrary(
        name = "symmetric_private_membership_wrapper",
        directoryPath = Paths.get(SWIG_PATH)
      )
    }
  }
}
