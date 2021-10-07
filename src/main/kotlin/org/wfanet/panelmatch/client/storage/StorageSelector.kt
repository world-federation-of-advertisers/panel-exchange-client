package org.wfanet.panelmatch.client.storage

import org.wfanet.measurement.api.v2alpha.ExchangeStepAttemptKey
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow

interface StorageSelector {

  suspend fun getPrivateStorage(attemptKey: ExchangeStepAttemptKey): VerifiedStorageClient

  suspend fun getSharedStorage(
    storageType: ExchangeWorkflow.StorageType,
    attemptKey: ExchangeStepAttemptKey,
    partnerName: String
  ): VerifiedStorageClient
}
