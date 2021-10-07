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
import com.google.protobuf.ByteString
import kotlin.reflect.KFunction1
import org.wfanet.measurement.api.v2alpha.ExchangeKey
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttemptKey
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.measurement.storage.StorageClient
import org.wfanet.panelmatch.common.certificates.CertificateManager
import org.wfanet.panelmatch.common.secrets.SecretMap

/**
 * Builds storage clients for the panel exchange workflow.
 *
 * [getStorageFactory]
 * - provides a serializable [StorageFactory] with details of the current exchange.
 * [getPrivateStorage]
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
class StorageSelector(
  private val sharedStorageInfo: SecretMap,
  private val privateStorageInfo: SecretMap,
  private val defaultPrivateStorageInfo: ByteString,
  private val certificateManager: CertificateManager,
  private val ownerName: String,
  private val storageFactories:
    ImmutableMap<StorageDetails.PlatformCase, KFunction1<StorageDetails, StorageFactory>>
) {

  private suspend fun getStorageFactory(storageDetails: StorageDetails): StorageFactory =
    requireNotNull(storageFactories[storageDetails.platformCase])(storageDetails)

  private suspend fun getStorageDetails(
    attemptKey: ExchangeStepAttemptKey,
    isShared: Boolean
  ): StorageDetails {
    return StorageDetails.parseFrom(
      if (isShared) {
        sharedStorageInfo.get(attemptKey.recurringExchangeId)
          ?: throw IllegalArgumentException("Shared storage details must be defined")
      } else {
        privateStorageInfo.get(attemptKey.recurringExchangeId) ?: defaultPrivateStorageInfo
      }
    )
  }

  /**
   * Gets the appropriate [StorageClient] for the current exchange. Requires the exchange to be
   * active with private storage recorded in our secret map.
   */
  suspend fun getPrivateStorage(attemptKey: ExchangeStepAttemptKey): StorageClient {
    return getStorageFactory(getStorageDetails(attemptKey, true)).build()
  }

  /**
   * Gets the appropriate [StorageFactory] for the current exchange. Requires the exchange to be
   * active with private storage recorded in our secret map. Note that since we only expect to need
   * a StorageFactory for private storage, this does not ever check [sharedStorageInfo].
   */
  suspend fun getStorageFactory(attemptKey: ExchangeStepAttemptKey): StorageFactory {
    return getStorageFactory(getStorageDetails(attemptKey, false))
  }

  /**
   * Makes an appropriate [VerifiedStorageClient] for the current exchange. Requires the exchange to
   * be active with shared storage recorded in our [sharedStorageInfo] secret map. Since shared
   * storage is the only storage that is verified, this is the only function that returns a
   * Verified client.
   *
   * [storageType] - grabbed from the exchange workflow to validate that our local information is
   * accurate.
   * [partnerName] - The API resource name of the partner in this exchange. Required to look up the
   * certificate required to validate reads.
   * [ownerCertificateResourceName] - Optional. The API resource name of the certificate we created
   * for this exchange. Only required for [CopyToSharedStorageTask] tasks, it is expected to be
   * passed through an input label for the tasks that need it. Tasks that do not write to shared
   * shared storage are expected to leave this as null so they don't need to depend on the task that
   * generates the certificate when they don't use it.
   */
  suspend fun getSharedStorage(
    storageType: ExchangeWorkflow.StorageType,
    attemptKey: ExchangeStepAttemptKey,
    partnerName: String,
    ownerCertificateResourceName: String?
  ): VerifiedStorageClient {
    val storageDetails = getStorageDetails(attemptKey, true)
    when (storageType) {
      ExchangeWorkflow.StorageType.GOOGLE_CLOUD_STORAGE -> requireNotNull(storageDetails.gcs)
      ExchangeWorkflow.StorageType.AMAZON_S3 -> requireNotNull(storageDetails.aws)
      else -> throw IllegalArgumentException("No supported shared storage type specified.")
    }

    return getVerifiedStorageClient(
      storageDetails,
      ExchangeKey(attemptKey.recurringExchangeId, attemptKey.exchangeId),
      partnerName,
      ownerCertificateResourceName
    )
  }

  private suspend fun getVerifiedStorageClient(
    storageDetails: StorageDetails,
    exchangeKey: ExchangeKey,
    partnerName: String,
    ownerCertificateResourceName: String?
  ): VerifiedStorageClient {

    return VerifiedStorageClient(
      storageClient = getStorageFactory(storageDetails).build(),
      exchangeKey = exchangeKey,
      ownerName,
      partnerName,
      ownerCertificateResourceName,
      certificateManager
    )
  }
}
