package org.wfanet.panelmatch.client.storage.testing

import java.util.concurrent.ConcurrentHashMap
import org.wfanet.measurement.storage.StorageClient
import org.wfanet.measurement.storage.testing.InMemoryStorageClient
import org.wfanet.panelmatch.common.secrets.testing.TestSecretMap

class TestPrivateStorageSelector {
  private val blobs = ConcurrentHashMap<String, StorageClient.Blob>()

  val storageClient = InMemoryStorageClient(blobs)
  val storageDetails = TestSecretMap()
  val selector = makeTestPrivateStorageSelector(storageDetails, storageClient)
}
