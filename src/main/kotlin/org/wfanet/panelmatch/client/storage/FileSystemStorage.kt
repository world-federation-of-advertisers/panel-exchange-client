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

import com.google.protobuf.ByteString
import java.io.File
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.reduce
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.measurement.storage.filesystem.FileSystemStorageClient

class FileSystemStorage(
  storageType: Storage.STORAGE_TYPE,
  label: String,
  step: ExchangeWorkflow.Step
) : Storage {
  private var storageClient: FileSystemStorageClient
  init {
    // This logic can be extended to use different file systems for different steps or labels
    val DEFAULT_SHARED_STORAGE_DIR = File("/tmp/")
    val DEFAULT_PRIVATE_STORAGE_DIR = File("/var/tmp/")
    storageClient =
      when (storageType) {
        Storage.STORAGE_TYPE.SHARED ->
          FileSystemStorageClient(directory = DEFAULT_SHARED_STORAGE_DIR)
        Storage.STORAGE_TYPE.PRIVATE ->
          FileSystemStorageClient(directory = DEFAULT_PRIVATE_STORAGE_DIR)
      }
  }

  override suspend fun read(path: String): ByteString {
    print("READ:${path}\n")
    return requireNotNull(storageClient.getBlob(path)).read(4096).reduce { a, b -> a.concat(b) }
  }

  override suspend fun write(path: String, data: ByteString) {
    print("WRITE:${path}\n")
    storageClient.createBlob(path, listOf(data).asFlow())
  }
}
