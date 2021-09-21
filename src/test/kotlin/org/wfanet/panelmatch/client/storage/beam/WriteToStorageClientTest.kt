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

package org.wfanet.panelmatch.client.storage.beam

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.measurement.common.base64UrlDecode
import org.wfanet.measurement.common.toByteString
import org.wfanet.measurement.storage.StorageClient
import org.wfanet.measurement.storage.filesystem.FileSystemStorageClient
import org.wfanet.panelmatch.client.storage.beam.WriteToStorageClient.Parameters
import org.wfanet.panelmatch.common.beam.testing.BeamTestBase
import org.wfanet.panelmatch.common.beam.testing.assertThat
import org.wfanet.panelmatch.common.toByteString

private const val PARAMETERS_PREFIX = "PARAMETERS_PREFIX"

@RunWith(JUnit4::class)
class WriteToStorageClientTest : BeamTestBase() {
  @get:Rule val temporaryFolder = TemporaryFolder()

  private val inputs = "ABCDEF"
  private val inputCollection by lazy {
    pcollectionOf("Input", *inputs.map { it.toString().toByteString() }.toTypedArray())
  }

  // TODO: this function relies too much on knowing the internal details of
  //  FileSystemStorageClient.
  //
  // However, until StorageClient has some type of List test-only helper, this seems like a better
  // option than re-implementing it.
  private fun runWithNumShards(numShards: Int): Map<String, String> {
    val baseDirectory: File = temporaryFolder.newFolder("beam-output").absoluteFile

    val storageClientProvider =
      object : WriteToStorageClient.StorageClientProvider {
        override fun get(): StorageClient {
          return FileSystemStorageClient(baseDirectory)
        }
      }

    val parameters = Parameters(numShards = numShards, prefix = PARAMETERS_PREFIX)
    val writeResult: WriteToStorageClient.WriteResult =
      WriteToStorageClient(storageClientProvider, parameters).expand(inputCollection)

    pipeline.run()

    val map: Map<String, String> =
      requireNotNull(baseDirectory.listFiles()).associate {
        val name = it.name.base64UrlDecode().toByteString().toStringUtf8()
        val contents = it.readBytes().toByteString().toStringUtf8()
        name to contents
      }

    assertThat(writeResult.blobKeys).satisfies {
      assertIsSubset(map.keys, it)
      null
    }

    assertThat(canonicalize(map.values)).isEqualTo(inputs)

    return map
  }

  @Test
  fun `one shard`() {
    val map = runWithNumShards(1)
    assertThat(map).hasSize(1)
  }

  @Test
  fun `many shards`() {
    val map = runWithNumShards(50)
    assertThat(map.size).isAtMost(inputs.length)
    val allPossibleShardNames = (0 until 50).map { "$PARAMETERS_PREFIX-%05d-of-00050".format(it) }
    assertIsSubset(map.keys, allPossibleShardNames)
  }
}

private fun canonicalize(elements: Iterable<String>): String {
  return elements.joinToString("").toList().sorted().joinToString("")
}

private fun assertIsSubset(subset: Iterable<*>, superset: Iterable<*>) {
  val subsetSet = subset.toSet()
  assertThat(subsetSet intersect superset.toSet()).containsExactlyElementsIn(subsetSet)
}
