package org.wfanet.panelmatch.client.storage

import java.security.PrivateKey
import java.security.cert.X509Certificate
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttemptKey
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow

interface StorageSelector {

  fun getPrivateStorage(
    attemptKey: ExchangeStepAttemptKey,
    ownedCertificate: X509Certificate,
    privateKey: PrivateKey
  ): VerifiedStorageClient

  fun getSharedStorage(
    storageType: ExchangeWorkflow.StorageType,
    attemptKey: ExchangeStepAttemptKey,
    partnerCertificate: X509Certificate,
    ownedCertificate: X509Certificate,
    privateKey: PrivateKey
  ): VerifiedStorageClient
}
