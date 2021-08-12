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

package org.wfanet.panelmatch.client.batchlookup

import java.nio.file.Paths
import org.wfanet.panelmatch.common.loadLibrary
import org.wfanet.panelmatch.common.wrapJniException
import org.wfanet.panelmatch.protocol.batchlookup.SwigObliviousQuery
import java.util.UUID.randomUUID

/** A [QueryCryptor] implementation using the JNI [SwigObliviousQuery]. */
class JniObliviousQueryBuilder : ObliviousQueryBuilder {

  override fun queryIdGenerator(panelistKey: PanelistKey): QueryId {
    return queryIdOf(randomUUID().getLeastSignificantBits().toInt())
  }

  override fun generateKeys(request: GenerateKeysRequest): GenerateKeysResponse {
    return wrapJniException {
      GenerateKeysResponse.parseFrom(SwigObliviousQuery.generateKeysWrapper(request.toByteArray()))
    }
  }

  override fun encryptQueries(request: EncryptQueriesRequest): EncryptQueriesResponse {
    return wrapJniException {
      EncryptQueriesResponse.parseFrom(
        SwigObliviousQuery.encryptQueriesWrapper(request.toByteArray())
      )
    }
  }

  override fun decryptQueries(request: DecryptQueriesRequest): DecryptQueriesResponse {
    return wrapJniException {
      DecryptQueriesResponse.parseFrom(
        SwigObliviousQuery.decryptQueriesWrapper(request.toByteArray())
      )
    }
  }

  companion object {

    init {
      val SWIG_PATH = "panel_exchange_client/src/main/swig/wfanet/panelmatch/protocol/batchlookup"
      loadLibrary(name = "oblivious_query", directoryPath = Paths.get(SWIG_PATH))
    }
  }
}
