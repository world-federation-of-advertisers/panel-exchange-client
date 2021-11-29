package org.wfanet.panelmatch.common.storage

import com.google.protobuf.ByteString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.wfanet.measurement.storage.StorageClient
import org.wfanet.measurement.storage.StorageClient.Blob

/** Deletes the blob for [blobKey] if it exists, then writes a new blob to [blobKey]. */
suspend fun StorageClient.createOrReplaceBlob(blobKey: String, contents: Flow<ByteString>): Blob {
  getBlob(blobKey)?.delete()
  return createBlob(blobKey, contents)
}

/** Deletes the blob for [blobKey] if it exists, then writes a new blob to [blobKey]. */
suspend fun StorageClient.createOrReplaceBlob(blobKey: String, contents: ByteString): Blob {
  return createOrReplaceBlob(blobKey, flowOf(contents))
}
