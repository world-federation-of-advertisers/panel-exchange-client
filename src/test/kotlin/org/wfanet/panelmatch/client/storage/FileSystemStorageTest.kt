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

import java.io.File
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.wfanet.measurement.common.crypto.readCertificate
import org.wfanet.measurement.common.crypto.readPrivateKey
import org.wfanet.measurement.common.crypto.testing.FIXED_SERVER_CERT_PEM_FILE as SERVER_CERT_PEM_FILE
import org.wfanet.measurement.common.crypto.testing.FIXED_SERVER_KEY_FILE as SERVER_KEY_FILE
import org.wfanet.measurement.common.crypto.testing.KEY_ALGORITHM
import org.wfanet.measurement.storage.filesystem.FileSystemStorageClient
import org.wfanet.panelmatch.client.storage.testing.AbstractStorageTest

class FileSystemStorageTest : AbstractStorageTest() {
  @get:Rule val temporaryFolder = TemporaryFolder()

  override val privateStorage by lazy {
    VerifiedStorageClient(
      FileSystemStorageClient(directory = File(temporaryFolder.newFolder("private").absolutePath)),
      readCertificate(SERVER_CERT_PEM_FILE),
      readCertificate(SERVER_CERT_PEM_FILE),
      readPrivateKey(SERVER_KEY_FILE, KEY_ALGORITHM)
    )
  }

  override val sharedStorage by lazy {
    VerifiedStorageClient(
      FileSystemStorageClient(directory = File(temporaryFolder.newFolder("shared").absolutePath)),
      readCertificate(SERVER_CERT_PEM_FILE),
      readCertificate(SERVER_CERT_PEM_FILE),
      readPrivateKey(SERVER_KEY_FILE, KEY_ALGORITHM)
    )
  }
}
