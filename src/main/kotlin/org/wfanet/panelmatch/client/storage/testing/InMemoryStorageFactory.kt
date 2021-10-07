package org.wfanet.panelmatch.client.storage.testing

import org.wfanet.measurement.storage.StorageClient
import org.wfanet.measurement.storage.testing.InMemoryStorageClient
import org.wfanet.panelmatch.client.storage.StorageDetails
import org.wfanet.panelmatch.client.storage.StorageFactory
import org.wfanet.panelmatch.client.storage.storageDetails

class InMemoryStorageFactory(private val storageDetails: StorageDetails) : StorageFactory {

  constructor() : this(storageDetails {})

  override fun build(): StorageClient {
    return InMemoryStorageClient()
  }
}
