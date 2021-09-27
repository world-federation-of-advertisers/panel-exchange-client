package org.wfanet.panelmatch.client.storage

import com.google.cloud.storage.StorageOptions
import com.google.protobuf.ByteString
import java.security.PrivateKey
import java.security.cert.X509Certificate
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttemptKey
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.measurement.gcloud.gcs.GcsStorageClient
import org.wfanet.measurement.storage.StorageClient

class StorageSelectorImpl(
  // TODO: Replace with SecretMap
  // Contains a map of recurring exchange id to the information required to
  // build the appropriate StorageClient for that exchange, serialized.
  private val sharedStorageInfo: Map<String, ByteString>,
  private val privateStorageInfo: Map<String, ByteString>,
  private val defaultPrivateStorageInfo: ByteString
) : StorageSelector {
  override fun getPrivateStorage(
    attemptKey: ExchangeStepAttemptKey,
    ownedCertificate: X509Certificate,
    privateKey: PrivateKey
  ): VerifiedStorageClient {
    val storageDetails: StorageDetails =
      StorageDetails.parseFrom(
        privateStorageInfo[attemptKey.recurringExchangeId] ?: defaultPrivateStorageInfo
      )

    return getStorageClient(
      storageDetails,
      "${attemptKey.recurringExchangeId}/${attemptKey.exchangeId}",
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
    val storageDetails: StorageDetails =
      requireNotNull(StorageDetails.parseFrom(sharedStorageInfo[attemptKey.recurringExchangeId]))
    when (storageType) {
      ExchangeWorkflow.StorageType.GOOGLE_CLOUD_STORAGE -> requireNotNull(storageDetails.gcs)
      ExchangeWorkflow.StorageType.AMAZON_S3 -> requireNotNull(storageDetails.aws)
      else -> throw IllegalArgumentException("No supported shared storage type specified.")
    }
    return getStorageClient(
      storageDetails,
      "${attemptKey.recurringExchangeId}/${attemptKey.exchangeId}",
      partnerCertificate,
      ownedCertificate,
      privateKey
    )
  }

  fun buildAws(storageDetails: StorageDetails): StorageClient {
    throw IllegalArgumentException("AWS Not yet implemented")
  }

  fun buildGcs(storageDetails: StorageDetails): GcsStorageClient {
    return GcsStorageClient(
      StorageOptions.newBuilder().setProjectId(storageDetails.gcs.projectName).build().service,
      storageDetails.gcs.bucket
    )
  }

  fun getStorageClient(
    storageDetails: StorageDetails,
    prefix: String,
    readCertificate: X509Certificate,
    writeCertificate: X509Certificate,
    privateKey: PrivateKey
  ): VerifiedStorageClient {
    val storageClient =
      when (storageDetails.platformCase) {
        StorageDetails.PlatformCase.AWS -> buildAws(storageDetails)
        StorageDetails.PlatformCase.GCS -> buildGcs(storageDetails)
        else -> throw IllegalArgumentException("Unsupported or no platform set.")
      }
    return VerifiedStorageClient(
      storageClient = storageClient,
      exchangePrefix = prefix,
      readCertificate,
      writeCertificate,
      privateKey
    )
  }
}
