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

package org.wfanet.panelmatch.client.storage.testing

import org.wfanet.measurement.api.v2alpha.ExchangeKey
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttemptKey
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.measurement.storage.StorageClient
import org.wfanet.measurement.storage.testing.InMemoryStorageClient
import org.wfanet.panelmatch.client.storage.StorageSelector
import org.wfanet.panelmatch.client.storage.VerifiedStorageClient
import org.wfanet.panelmatch.common.testing.TestCertificateManager

class TestStorageSelector(private val underlyingClient: StorageClient = InMemoryStorageClient()) :
  StorageSelector {

  override suspend fun getPrivateStorage(
    attemptKey: ExchangeStepAttemptKey,
  ): StorageClient {
    return underlyingClient
  }

  override suspend fun getSharedStorage(
    storageType: ExchangeWorkflow.StorageType,
    attemptKey: ExchangeStepAttemptKey,
    partnerName: String,
    ownerCertificateResourceName: String?
  ): VerifiedStorageClient {
    return makeTestVerifiedStorageClient(underlyingClient)
  }
}

fun makeTestVerifiedStorageClient(
  underlyingClient: StorageClient = InMemoryStorageClient()
): VerifiedStorageClient {
  return VerifiedStorageClient(
    underlyingClient,
    ExchangeKey("test", "prefix"),
    "owner",
    "partner",
    "ownerCert",
    TestCertificateManager()
  )
}
