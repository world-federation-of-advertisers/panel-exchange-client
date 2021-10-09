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

package org.wfanet.panelmatch.common

import com.google.protobuf.ByteString
import org.wfanet.measurement.common.asBufferedFlow
import org.wfanet.measurement.common.flatten
import org.wfanet.measurement.storage.StorageClient
import org.wfanet.measurement.storage.StorageClient.Blob
import org.wfanet.measurement.storage.read

/** Reads the blob into a single [ByteString]. */
suspend fun Blob.toByteString(): ByteString = read().flatten()

/** Reads the blob bytes as a UTF8 [String]. */
suspend fun Blob.toStringUtf8(): String = toByteString().toStringUtf8()

/** Creates a [StorageClient.Blob] from a [ByteString]. */
suspend fun StorageClient.createBlob(blobKey: String, content: ByteString): Blob {
  return createBlob(blobKey, content.asBufferedFlow(this.defaultBufferSizeBytes))
}