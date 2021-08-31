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
import java.lang.Exception
import java.security.PrivateKey
import java.security.cert.X509Certificate
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wfanet.measurement.common.flatten
import org.wfanet.measurement.storage.StorageClient
import org.wfanet.measurement.storage.StorageClient.Blob
import org.wfanet.measurement.storage.read

class StorageNotFoundException(inputKey: String) : Exception("$inputKey not found")

open class VerifiedStorageClient(
  private val storageClient: StorageClient,
  private val readCert: X509Certificate,
  private val writeCert: X509Certificate,
  private val privateKey: PrivateKey
) {

  val defaultBufferSizeBytes: Int = storageClient.defaultBufferSizeBytes

  /**
   * Transforms values of [inputLabels] into the underlying blobs.
   *
   * If any blob can't be found, it throws [NotFoundException].
   *
   * All files read are verified against the appropriate [X509Certificate] to validate that the
   * files came from the expected source.
   */
  @Throws(StorageNotFoundException::class)
  suspend fun verifiedBatchRead(inputLabels: Map<String, String>): Map<String, VerifiedBlob> {
    // create an immutable copy of inputLabels to avoid race conditions if the underlying label map
    // is changed during execution.
    val immutableInputs: ImmutableMap<String, String> = ImmutableMap.copyOf(inputLabels)

    return withContext(Dispatchers.IO) {
      coroutineScope {
        immutableInputs
          .mapValues { entry ->
            async(start = CoroutineStart.DEFAULT) {
              this@VerifiedStorageClient.getBlob(blobKey = entry.value)
            }
          }
          .mapValues { entry -> entry.value.await() }
      }
    }
  }

  /**
   * Writes output [data] based on [outputLabels].
   *
   * All outputs written by this function are signed by the user's PrivateKey, which is also written
   * as a separate file.
   */
  suspend fun verifiedBatchWrite(
    outputLabels: Map<String, String>,
    data: Map<String, Flow<ByteString>>
  ) {
    // create an immutable copy of outputLabels to avoid race conditions if the underlying label map
    // is changed during execution.
    val immutableOutputs: ImmutableMap<String, String> = ImmutableMap.copyOf(outputLabels)
    require(immutableOutputs.values.toSet().size == immutableOutputs.size) {
      "Cannot write to the same output location twice"
    }
    withContext(Dispatchers.IO) {
      coroutineScope {
        for ((key, value) in immutableOutputs) {
          val payload = requireNotNull(data[key]) { "Key $key not found in ${data.keys}" }
          launch { this@VerifiedStorageClient.createBlob(blobKey = value, content = payload) }
        }
      }
    }
  }

  /**
   * Replacement for StorageClient.getblob(). Intended to be used in combination with another
   * party's provided [X509Certificate], this creates a [VerifiedBlob] that will check that the data
   * in the blob has been generated (or at least signed as valid) by the other party.
   *
   * Notes:
   * - The validation happens in a separate thread and is non-blocking, but will throw a
   * ```
   *    terminal error if it fails.
   * ```
   * - The validation does not happen until
   */
  @Throws(StorageNotFoundException::class)
  suspend fun getBlob(blobKey: String): VerifiedBlob {
    // TODO: downstream PR to implement signature validation
    //  Downstream reads the file as well as a signature file derived from blobKey and starts a
    //  validation process using a provided [X509Certificate]. Right now it just runs [getBlob].
    //  This function assumes that StorageClient will handle tracking the [X509Certificate].
    val sourceBlob: Blob = storageClient.getBlob(blobKey) ?: throw StorageNotFoundException(blobKey)
    return VerifiedBlob(sourceBlob, blobKey, readCert)
  }

  /**
   * Stub for verified write function. Intended to be used in combination with a provided
   * [PrivateKey] , this creates a signature in shared storage for the written blob that can be
   * verified by the other party using a pre-provided [X509Certificate].
   */
  suspend fun createBlob(blobKey: String, content: Flow<ByteString>): VerifiedBlob {
    // TODO: downstream PR to implement signing
    //  This is intended to actually write two files (by creating two Blobs). One being the normal
    //  write, the other writing a signature file created with our [PrivateKey]. We still only
    // return
    //  the Blob created from the passed [content] val, but a failure to sign the file will still
    //  throw an exception (causing the task step to fail).
    //  This function assumes that StorageClient will handle tracking the [PrivateKey].
    return VerifiedBlob(
      storageClient.createBlob(blobKey = blobKey, content = content),
      blobKey,
      writeCert
    )
  }

  open class VerifiedBlob(
    private val sourceBlob: Blob,
    val blobKey: String,
    private val cert: X509Certificate
  ) {

    fun size(): Long {
      return sourceBlob.size
    }

    fun defaultBufferSizeBytes(): Int {
      return sourceBlob.storageClient.defaultBufferSizeBytes
    }

    /**
     * Stub for verified read function. Intended to be used in combination with a the other party's
     * provided [X509Certificate], this validates that the data in the blob has been generated (or
     * at least signed as valid) by the other party.
     *
     * Note that the validation happens in a separate thread and is non-blocking, but will throw a
     * terminal error if it fails.
     */
    private fun verifiedRead(bufferSize: Int): Flow<ByteString> {
      return sourceBlob.read(bufferSize)
    }

    fun read(bufferSize: Int): Flow<ByteString> {
      return verifiedRead(bufferSize)
    }

    fun read(): Flow<ByteString> {
      return verifiedRead(defaultBufferSizeBytes())
    }

    suspend fun toByteString(): ByteString = this.read().flatten()
  }
}

// TODO: add this as a method to StorageClient.kt as StorageClient.Blob.toByteString
/**
 * Aggregates the [Flow] contained within a [StorageClient.Blob] object into a single concatenated
 * [ByteString].
 */
suspend fun Blob.toByteString(): ByteString = this.read().flatten()
