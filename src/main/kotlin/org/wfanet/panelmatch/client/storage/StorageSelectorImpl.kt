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

import com.google.cloud.storage.StorageOptions
import com.google.protobuf.ByteString
import org.wfanet.measurement.api.v2alpha.ExchangeKey
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttemptKey
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.measurement.gcloud.gcs.GcsStorageClient
import org.wfanet.measurement.storage.StorageClient
import org.wfanet.panelmatch.common.CertificateManager

class StorageSelectorImpl(
  // TODO: Replace with SecretMap
  // Contains a map of recurring exchange id to the information required to
  // build the appropriate StorageClient for that exchange, serialized.
  private val sharedStorageInfo: Map<String, ByteString>,
  private val privateStorageInfo: Map<String, ByteString>,
  private val defaultPrivateStorageInfo: ByteString,
  private val certificateManager: CertificateManager,
  private val ownerName: String,
) : StorageSelector {

  // TODO: refactor this to return either a regular StorageClient or non-verified wrapper (as we
  //  still want support for automatic prefixes and batch I/O).
  override suspend fun getPrivateStorage(attemptKey: ExchangeStepAttemptKey): StorageClient {

    val storageDetails: StorageDetails =
      StorageDetails.parseFrom(
        privateStorageInfo[attemptKey.recurringExchangeId] ?: defaultPrivateStorageInfo
      )

    return getStorageClient(storageDetails)
  }

  override suspend fun getSharedStorage(
    storageType: ExchangeWorkflow.StorageType,
    attemptKey: ExchangeStepAttemptKey,
    partnerName: String,
    ownerCertificateResourceName: String?
  ): VerifiedStorageClient {
    val storageDetails: StorageDetails =
      requireNotNull(StorageDetails.parseFrom(sharedStorageInfo[attemptKey.recurringExchangeId]))
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

  private fun buildAws(storageDetails: StorageDetails): StorageClient {
    throw IllegalArgumentException("AWS Not yet implemented")
  }

  private fun buildGcs(storageDetails: StorageDetails): GcsStorageClient {
    // TODO(jonmolle): Implement rotating bucket option.
    return GcsStorageClient(
      StorageOptions.newBuilder().setProjectId(storageDetails.gcs.projectName).build().service,
      storageDetails.gcs.bucket
    )
  }

  private suspend fun getVerifiedStorageClient(
    storageDetails: StorageDetails,
    exchangeKey: ExchangeKey,
    partnerName: String,
    ownerCertificateResourceName: String?
  ): VerifiedStorageClient {

    return VerifiedStorageClient(
      storageClient = getStorageClient(storageDetails),
      exchangeKey = exchangeKey,
      ownerName,
      partnerName,
      ownerCertificateResourceName,
      certificateManager
    )
  }

  private fun getStorageClient(
    storageDetails: StorageDetails,
  ): StorageClient {
    return when (storageDetails.platformCase) {
      StorageDetails.PlatformCase.AWS -> buildAws(storageDetails)
      StorageDetails.PlatformCase.GCS -> buildGcs(storageDetails)
      else -> throw IllegalArgumentException("Unsupported or no platform set.")
    }
  }
}
