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
class FileSystemStorage(baseDir: String, label: String) : Storage {
  companion object {
    val logger by loggerFor()
  }
  private var storageClient: FileSystemStorageClient
  init {
    val BASE_DIR = File(baseDir)
    storageClient = FileSystemStorageClient(directory = BASE_DIR)
  }

  override suspend fun read(path: String): ByteString {
    logger.info("Read:${path}\n")
    return requireNotNull(storageClient.getBlob(path)).read(4096).reduce { a, b -> a.concat(b) }
  }

  override suspend fun write(path: String, data: ByteString) {
    logger.info("Write:${path}\n")
    storageClient.createBlob(path, listOf(data).asFlow())
  }
}
