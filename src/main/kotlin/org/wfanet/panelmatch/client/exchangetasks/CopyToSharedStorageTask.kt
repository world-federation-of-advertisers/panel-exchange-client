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
import org.wfanet.measurement.storage.StorageClient
import org.wfanet.measurement.storage.StorageClient.Blob
import org.wfanet.panelmatch.client.storage.SigningStorageClient

/** Implements CopyToSharedStorageStep for regular (non-manifest) blobs. */
class CopyToSharedStorageTask(
  private val source: StorageClient,
  private val destination: SigningStorageClient,
  private val copyOptions: CopyOptions,
  private val sourceBlobKey: String,
  private val destinationBlobKey: String,
) : CustomIOExchangeTask() {
  override suspend fun execute() {
    require(copyOptions.labelType == BLOB) { "Unsupported CopyOptions: $copyOptions" }

    val sourceBlob: Blob =
      requireNotNull(source.getBlob(sourceBlobKey)) { "Missing blob with key: $sourceBlobKey" }
    destination.writeBlob(destinationBlobKey, sourceBlob.read())
  }
}
