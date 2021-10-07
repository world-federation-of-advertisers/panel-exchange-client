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

import com.google.common.collect.ImmutableMap
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttemptKey
import org.wfanet.measurement.storage.StorageClient
import org.wfanet.panelmatch.common.secrets.SecretMap

/**
 * Builds storage clients for the panel exchange workflow.
 *
 * [getStorageFactory]
 * - provides a serializable [StorageFactory] with details of the current exchange.
 * [getStorageClient]
 * - provides a [StorageClient] [getSharedStorage]
 * - provides a [VerifiedStorageClient]
 *
 * The class takes in exchange-specific storage information ([sharedStorageInfo] and
 * [privateStorageInfo]) that is required to build the appropriate storage clients for each
 * exchange. We expect these values to be set when an exchange is first created and are not shared
 * with the Kingdom. These are currently keyed by the [ExchangeStepAttemptKey.recurringExchangeId].
 *
 * [storageFactories] is a map of storage factory constructors supported by our daemon. As not all
 * types of StorageClients will necessarily be supported by all EDPs and MPs, this gives them the
 * option to not depend on the ones they choose not to support.
 */
class PrivateStorageSelector(
  private val privateStorageFactories:
    ImmutableMap<StorageDetails.PlatformCase, (StorageDetails) -> StorageFactory>,
  private val privateStorageInfo: SecretMap
) {

  private suspend fun getStorageFactory(storageDetails: StorageDetails): StorageFactory {
    val storageFactoryBuilder =
      requireNotNull(privateStorageFactories[storageDetails.platformCase]) {
        "Missing private StorageFactory for ${storageDetails.platformCase}"
      }
    return storageFactoryBuilder(storageDetails)
  }

  private suspend fun getStorageDetails(attemptKey: ExchangeStepAttemptKey): StorageDetails {
    val storageDetails =
      StorageDetails.parseFrom(
        privateStorageInfo.get(attemptKey.recurringExchangeId)
          ?: throw StorageNotFoundException(
            "No shared storage found for exchange ${attemptKey.recurringExchangeId}"
          )
      )

    require(storageDetails.visibility == StorageDetails.Visibility.PRIVATE)
    return storageDetails
  }

  /**
   * Gets the appropriate [StorageFactory] for the current exchange. Requires the exchange to be
   * active with private storage recorded in our secret map. Note that since we only expect to need
   * a StorageFactory for private storage, this does not ever check [sharedStorageInfo].
   */
  suspend fun getStorageFactory(attemptKey: ExchangeStepAttemptKey): StorageFactory {
    return getStorageFactory(getStorageDetails(attemptKey))
  }

  /**
   * Gets the appropriate [StorageClient] for the current exchange. Requires the exchange to be
   * active with private storage recorded in our secret map.
   */
  suspend fun getStorageClient(attemptKey: ExchangeStepAttemptKey): StorageClient {
    return getStorageFactory(attemptKey).build()
  }
}
