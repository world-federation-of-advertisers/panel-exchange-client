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

import com.google.common.collect.ImmutableMap
import org.wfanet.measurement.api.v2alpha.ExchangeKey
import org.wfanet.measurement.storage.StorageClient
import org.wfanet.measurement.storage.testing.InMemoryStorageClient
import org.wfanet.panelmatch.client.storage.PrivateStorageSelector
import org.wfanet.panelmatch.client.storage.SharedStorageSelector
import org.wfanet.panelmatch.client.storage.StorageDetails
import org.wfanet.panelmatch.client.storage.VerifiedStorageClient
import org.wfanet.panelmatch.common.certificates.testing.TestCertificateManager
import org.wfanet.panelmatch.common.secrets.SecretMap

fun makeTestPrivateStorageSelector(
  secretMap: SecretMap,
  underlyingClient: InMemoryStorageClient
): PrivateStorageSelector {

  val rootInMemoryStorageFactory = InMemoryStorageFactory(underlyingClient)

  return PrivateStorageSelector(
    ImmutableMap.of(
      StorageDetails.PlatformCase.FILE,
      rootInMemoryStorageFactory::testBuilder,
      StorageDetails.PlatformCase.AWS,
      rootInMemoryStorageFactory::testBuilder,
      StorageDetails.PlatformCase.GCS,
      rootInMemoryStorageFactory::testBuilder,
      StorageDetails.PlatformCase.PLATFORM_NOT_SET,
      rootInMemoryStorageFactory::testBuilder,
    ),
    secretMap
  )
}

fun makeTestSharedStorageSelector(
  secretMap: SecretMap,
  underlyingClient: InMemoryStorageClient
): SharedStorageSelector {

  val rootInMemoryStorageFactory = InMemoryStorageFactory(underlyingClient)

  return SharedStorageSelector(
    TestCertificateManager(),
    "owner",
    ImmutableMap.of(
      StorageDetails.PlatformCase.FILE,
      rootInMemoryStorageFactory::testBuilder,
      StorageDetails.PlatformCase.AWS,
      rootInMemoryStorageFactory::testBuilder,
      StorageDetails.PlatformCase.GCS,
      rootInMemoryStorageFactory::testBuilder,
      StorageDetails.PlatformCase.PLATFORM_NOT_SET,
      rootInMemoryStorageFactory::testBuilder,
    ),
    secretMap
  )
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
