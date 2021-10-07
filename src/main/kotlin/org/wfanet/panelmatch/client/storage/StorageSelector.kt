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

class StorageSelector(
  // Contains a map of recurring exchange id to the information required to
  // build the appropriate StorageClient for that exchange, serialized.
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
    isShared: Boolean = false
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

  suspend fun getPrivateStorage(attemptKey: ExchangeStepAttemptKey): StorageClient {
    return getStorageFactory(getStorageDetails(attemptKey, true)).build()
  }

  suspend fun getStorageFactory(
    attemptKey: ExchangeStepAttemptKey,
    isPrivate: Boolean
  ): StorageFactory {
    return getStorageFactory(getStorageDetails(attemptKey, isPrivate))
  }

  suspend fun getSharedStorage(
    storageType: ExchangeWorkflow.StorageType,
    attemptKey: ExchangeStepAttemptKey,
    partnerName: String,
    ownerCertificateResourceName: String?
  ): VerifiedStorageClient {
    val storageDetails = getStorageDetails(attemptKey)
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
