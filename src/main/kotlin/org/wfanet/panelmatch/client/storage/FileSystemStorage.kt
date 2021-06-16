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
import org.wfanet.measurement.storage.filesystem.FileSystemStorageClient
import org.wfanet.panelmatch.client.logger.loggerFor

/**
 * Reads input data from given path from the File System.
 *
 * @param baseDir String directory to read/write.
 * @param label String name of file to read/write
 */
class FileSystemStorage(storageType: Storage.STORAGE_TYPE, label: String) : Storage {
  private var storageClient: FileSystemStorageClient
  init {
    /**
     * FileSystemStorage needs two different folders: One for private storage and shared storage
     * TODO: Read these values based on a local config
     */
    val baseFolder =
      when (storageType) {
        Storage.STORAGE_TYPE.SHARED -> File("/tmp/panel-match/private-storage")
        Storage.STORAGE_TYPE.PRIVATE -> File("/tmp/panel-match/shared-storage")
      }
    if (!baseFolder.exists()) {
      if (!baseFolder.getParentFile().exists()) {
        baseFolder.getParentFile().mkdir()
      }
      baseFolder.mkdir()
    }
    storageClient = FileSystemStorageClient(directory = baseFolder)
  }

  override suspend fun read(path: String): ByteString {
    logger.info("Read:${path}\n")
    return requireNotNull(storageClient.getBlob(path)).read(4096).reduce { a, b -> a.concat(b) }
  }

  override suspend fun write(path: String, data: ByteString) {
    logger.info("Write:${path}\n")
    storageClient.createBlob(path, listOf(data).asFlow())
  }
  companion object {
    val logger by loggerFor()
  }
}
