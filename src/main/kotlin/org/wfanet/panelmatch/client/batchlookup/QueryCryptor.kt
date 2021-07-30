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

import org.wfanet.panelmatch.client.batchlookup.ObliviousQueryParameters
import org.wfanet.panelmatch.client.batchlookup.GenerateKeysRequest
import org.wfanet.panelmatch.client.batchlookup.GenerateKeysResponse
import org.wfanet.panelmatch.client.batchlookup.UnencryptedQuery
import org.wfanet.panelmatch.client.batchlookup.EncryptQueriesRequest
import org.wfanet.panelmatch.client.batchlookup.EncryptedQuery
import org.wfanet.panelmatch.client.batchlookup.EncryptQueriesResponse
import org.wfanet.panelmatch.client.batchlookup.DecryptQueriesRequest
import org.wfanet.panelmatch.client.batchlookup.DecryptQueriesResponse

interface QueryCryptor {

  fun encrypteQueries(request: EncryptQueriesRequest): EncryptQueriesResponse

  fun generateKeys(request: GenerateKeysRequest): GenerateKeysResponse
}

