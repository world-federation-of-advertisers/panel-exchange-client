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

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.wfanet.measurement.storage.filesystem.FileSystemStorageClient
import org.wfanet.panelmatch.client.storage.testing.VerifiedStorageClientTest
import org.wfanet.panelmatch.client.storage.testing.makeTestVerifiedStorageClient

class FileSystemVerifiedStorageClientTest : VerifiedStorageClientTest() {
  @get:Rule val temporaryFolder = TemporaryFolder()

  override val storage by lazy {
    temporaryFolder.create()
    makeTestVerifiedStorageClient(FileSystemStorageClient(directory = temporaryFolder.root))
  }
}