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

package org.wfanet.panelmatch.client.storage

import org.wfanet.measurement.api.v2alpha.ExchangeStepAttemptKey
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.measurement.storage.StorageClient

/**
 * This build the appropriate StorageClient and/or VerifiedStorageClient for each exchange task.
 *
 * It must depend on some static secret management (whether that is a private blob store or secret
 * manager is not expected to be handled here) to cache and look up certificates and private keys
 * for each exchange.
 */
interface StorageSelector {

  /** Builds the appropriate private StorageClient for the current exchange */
  suspend fun getPrivateStorage(attemptKey: ExchangeStepAttemptKey): StorageClient

  /**
   * Builds a a VerifiedStorageClient that has access to the current recurring exchange's shared
   * storage.
   *
   * As we only expect to know (or care) about what the certificate we use to _sign_ our shared
   * outputs with is when we are writing to shared storage, we don't require that resource name to
   * instantiate shared storage, as we only expect to know it in for the tasks we are writing out to
   * shared storage.
   */
  suspend fun getSharedStorage(
    storageType: ExchangeWorkflow.StorageType,
    attemptKey: ExchangeStepAttemptKey,
    partnerName: String,
    ownerCertificateResourceName: String?
  ): VerifiedStorageClient
}
