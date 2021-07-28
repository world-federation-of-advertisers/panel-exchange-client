// Copyright 2020 The Cross-Media Measurement Authors
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

import com.google.protobuf.ByteString
import java.io.File
import java.net.URI
import kotlinx.coroutines.flow.Flow
import org.wfanet.measurement.common.BYTES_PER_MIB
import org.wfanet.measurement.common.asBufferedFlow
import org.wfanet.measurement.storage.StorageClient
import org.wfanet.measurement.storage.filesystem.FileSystemStorageClient
import org.wfanet.panelmatch.client.logger.loggerFor
import org.wfanet.panelmatch.client.storage.Storage
import org.wfanet.panelmatch.protocol.common.Cryptor
import org.wfanet.panelmatch.protocol.common.JniDeterministicCommutativeCryptor

/**
 * Size of byte buffer used to read/write blobs from Google Cloud Storage.
 *
 * The optimal size is suggested by
 * [this article](https://medium.com/@duhroach/optimal-size-of-a-cloud-storage-fetch-8c270b511016).
 */
private const val BYTE_BUFFER_SIZE = BYTES_PER_MIB * 1

/**
 * Cloud Storage Signed URL implementation of [StorageClient] for a single object. Due to how signed
 * urls are implemented, this only supports one type of operation per instance.
 */
class RemoteUrlStorage(
  // The encrypted URL that is backing this storage abstraction
  // private val encryptedSignedUrl: ByteString,
  // URLs only support one action (in our case, read or write).
  private val operationType: String,
  // The key used to decrypt the signedUrl.
  private val inputKey: ByteString,
) : Storage {

  // TODO: replace this with the proper HTTP Storage Client
  private val storageClient: StorageClient = FileSystemStorageClient(directory = File("file"))
  private val cryptor: Cryptor = JniDeterministicCommutativeCryptor()

  private fun decodeURL(encryptedUrl: String): URI {
    val signedUrlString: String =
      cryptor.decrypt(inputKey, listOf<ByteString>(ByteString.copyFromUtf8(encryptedUrl)))[0]
        .toStringUtf8()
    return URI.create(signedUrlString)
  }

  override suspend fun write(path: String, data: Flow<ByteString>) {
    check(operationType == "write") {
      """
        Not allowed to read from a URL tagged as in $operationType mode. Signed Urls only support
        one type of operation.
        """.trimIndent()
    }

    val signedUrl: URI = decodeURL(path)
    logger.fine("Write:${signedUrl.authority}\n")

    // TODO: replace this with createBlobFromUrl when Client is implemented
    storageClient.createBlob(signedUrl.toString(), data.asBufferedFlow(BYTE_BUFFER_SIZE))
  }

  override suspend fun read(path: String): Flow<ByteString> {
    check(operationType == "read") {
      """
      Not allowed to read from a URL tagged as in $operationType mode. Signed Urls only support
      one type of operation.
      """.trimIndent()
    }

    var signedUrl: URI = decodeURL(path)
    logger.fine("Read:${signedUrl.authority}\n")

    // TODO: replace this with createBlobFromUrl when Client is implemented
    val blob = storageClient.getBlob(signedUrl.toString()) ?: throw Storage.NotFoundException(path)
    return blob.read(BYTE_BUFFER_SIZE)
  }

  companion object {
    private val logger by loggerFor()
  }

  /* override fun delete() {
    throw IllegalStateException("Deletion is not supported for signed url-based storage.")
  }*/
}
