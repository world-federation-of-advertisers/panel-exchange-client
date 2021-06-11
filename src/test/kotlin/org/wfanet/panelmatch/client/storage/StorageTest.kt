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

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import org.junit.Test

class StorageTest {

  @Test
  fun `write and read FileSystemStorage`() = runBlocking {
    val valueToStore = ByteString.copyFromUtf8("random-edp-string-0")
    val key = java.util.UUID.randomUUID().toString()
    val storage = FileSystemStorage(baseDir = "/tmp", label = key)
    storage.write(key, valueToStore)
    assertThat(storage.read(key)).isEqualTo(valueToStore)
  }

  @Test
  fun `get error for invalid key from FileSystemStorage`() = runBlocking {
    val valueToStore = ByteString.copyFromUtf8("random-edp-string-0")
    val key = java.util.UUID.randomUUID().toString()
    val storage = FileSystemStorage(baseDir = "/tmp", label = key)
    val reencryptException = assertFailsWith(IllegalArgumentException::class) { storage.read(key) }
  }

  @Test
  fun `get error for rewriting to same key 2x in FileSystemStorage`() = runBlocking {
    val valueToStore1 = ByteString.copyFromUtf8("random-edp-string-1")
    val valueToStore2 = ByteString.copyFromUtf8("random-edp-string-2")
    val key = java.util.UUID.randomUUID().toString()
    val storage = FileSystemStorage(baseDir = "/tmp", label = key)
    storage.write(key, valueToStore1)
    val doubleWriteException =
      assertFailsWith(IllegalArgumentException::class) { storage.write(key, valueToStore1) }
  }

  @Test
  fun `read to one baseDir and make sure you cannot read it from another baseDir`() = runBlocking {
    val valueToStore1 = ByteString.copyFromUtf8("random-edp-string-1")
    val valueToStore2 = ByteString.copyFromUtf8("random-edp-string-2")
    val key = java.util.UUID.randomUUID().toString()
    val privateStorage = FileSystemStorage(baseDir = "/tmp", label = key)
    privateStorage.write(key, valueToStore1)
    val storedValue = privateStorage.read(key)
    val sharedStorage = FileSystemStorage(baseDir = "/var/tmp", label = key)
    val doubleWriteException =
      assertFailsWith(IllegalArgumentException::class) { sharedStorage.read(key) }
  }
}
