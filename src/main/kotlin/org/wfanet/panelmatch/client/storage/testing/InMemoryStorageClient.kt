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

package org.wfanet.panelmatch.client.storage.testing

import com.google.protobuf.ByteString
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.Flow
import org.wfanet.measurement.common.BYTES_PER_MIB
import org.wfanet.measurement.common.asBufferedFlow
import org.wfanet.measurement.common.flatten
import org.wfanet.measurement.storage.StorageClient

/**
 * The default byte buffer size. Chosen as it is a commonly used default buffer size in an attempt
 * to keep the tests as close to actual usage as possible.
 */
private const val BYTE_BUFFER_SIZE = BYTES_PER_MIB * 1

/** [StorageClient] for InMemoryStorage service. */
class InMemoryStorageClient(
  private var storageMap: ConcurrentHashMap<String, StorageClient.Blob> =
    ConcurrentHashMap<String, StorageClient.Blob>()
) : StorageClient {

  private fun deleteKey(path: String) {
    storageMap.remove(path)
  }

  override val defaultBufferSizeBytes: Int
    get() = BYTE_BUFFER_SIZE

  override suspend fun createBlob(blobKey: String, content: Flow<ByteString>): StorageClient.Blob {
    require(!storageMap.containsKey(blobKey)) { "Cannot write to an existing key: $blobKey" }

    // As we're using this primarily for unit tests, we want to collect the input to record
    // size and to emulate "writing out" to memory.
    val newBlob = Blob(content.flatten(), blobKey)
    storageMap[blobKey] = newBlob

    return newBlob
  }

  override fun getBlob(blobKey: String): StorageClient.Blob? {
    return storageMap[blobKey]
  }

  private inner class Blob(private val byteData: ByteString, private val blobKey: String) :
    StorageClient.Blob {

    override val size: Long = byteData.size().toLong()

    override val storageClient: InMemoryStorageClient = this@InMemoryStorageClient

    override fun read(bufferSizeBytes: Int): Flow<ByteString> =
      byteData.asBufferedFlow(bufferSizeBytes)

    override fun delete() = storageClient.deleteKey(blobKey)
  }
}
