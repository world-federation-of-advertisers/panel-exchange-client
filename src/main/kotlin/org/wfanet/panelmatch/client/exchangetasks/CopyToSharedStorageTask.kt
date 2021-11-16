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

package org.wfanet.panelmatch.client.exchangetasks

import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow.Step.CopyOptions
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow.Step.CopyOptions.LabelType.BLOB
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow.Step.CopyOptions.LabelType.MANIFEST
import org.wfanet.measurement.storage.StorageClient
import org.wfanet.measurement.storage.StorageClient.Blob
import org.wfanet.measurement.storage.read
import org.wfanet.panelmatch.client.storage.VerifiedStorageClient
import org.wfanet.panelmatch.common.ShardedFileName
import org.wfanet.panelmatch.common.storage.toByteString

/** Implements CopyToSharedStorageStep. */
class CopyToSharedStorageTask(
  private val source: StorageClient,
  private val destination: VerifiedStorageClient,
  private val copyOptions: CopyOptions,
  private val sourceBlobKey: String,
  private val destinationBlobKey: String
) : CustomIOExchangeTask() {
  override suspend fun execute() {
    val blob = readBlob(sourceBlobKey)

    when (copyOptions.labelType) {
      BLOB -> blob.copyExternally(destinationBlobKey)
      MANIFEST -> writeManifestOutputs(blob)
      else -> error("Unrecognized CopyOptions: $copyOptions")
    }
  }

  private suspend fun writeManifestOutputs(blob: Blob) {
    val manifestBytes = blob.toByteString()
    val shardedFileName = ShardedFileName(manifestBytes.toStringUtf8())

    destination.createBlob(destinationBlobKey, manifestBytes)

    for (shardName in shardedFileName.fileNames) {
      readBlob(shardName).copyExternally(shardName)
    }
  }

  private fun readBlob(blobKey: String): Blob {
    return requireNotNull(source.getBlob(blobKey)) { "Missing blob with key: $blobKey" }
  }

  private suspend fun Blob.copyExternally(destinationBlobKey: String) {
    destination.createBlob(destinationBlobKey, read())
  }
}