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

import java.security.PrivateKey
import java.security.cert.X509Certificate
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttemptKey
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.measurement.common.crypto.readCertificate
import org.wfanet.measurement.common.crypto.readPrivateKey
import org.wfanet.measurement.common.crypto.testing.FIXED_SERVER_CERT_PEM_FILE
import org.wfanet.measurement.common.crypto.testing.FIXED_SERVER_KEY_FILE
import org.wfanet.measurement.common.crypto.testing.KEY_ALGORITHM
import org.wfanet.measurement.storage.StorageClient
import org.wfanet.measurement.storage.testing.InMemoryStorageClient
import org.wfanet.panelmatch.client.storage.StorageSelector
import org.wfanet.panelmatch.client.storage.VerifiedStorageClient

private val certificate: X509Certificate by lazy { readCertificate(FIXED_SERVER_CERT_PEM_FILE) }

private val privateKey: PrivateKey by lazy { readPrivateKey(FIXED_SERVER_KEY_FILE, KEY_ALGORITHM) }

class TestStorageSelector(private val underlyingClient: StorageClient = InMemoryStorageClient()) :
  StorageSelector {

  override fun getPrivateStorage(
    attemptKey: ExchangeStepAttemptKey,
    ownedCertificate: X509Certificate,
    privateKey: PrivateKey
  ): VerifiedStorageClient {
    return VerifiedStorageClient(
      underlyingClient,
      "test-prefix",
      ownedCertificate,
      ownedCertificate,
      privateKey
    )
  }

  override fun getSharedStorage(
    storageType: ExchangeWorkflow.StorageType,
    attemptKey: ExchangeStepAttemptKey,
    partnerCertificate: X509Certificate,
    ownedCertificate: X509Certificate,
    privateKey: PrivateKey
  ): VerifiedStorageClient {
    return VerifiedStorageClient(
      underlyingClient,
      "test-prefix",
      partnerCertificate,
      ownedCertificate,
      privateKey
    )
  }
}

fun makeTestVerifiedStorageClient(
  underlyingClient: StorageClient = InMemoryStorageClient()
): VerifiedStorageClient {
  return VerifiedStorageClient(
    underlyingClient,
    "test-prefix",
    certificate,
    certificate,
    privateKey
  )
}
