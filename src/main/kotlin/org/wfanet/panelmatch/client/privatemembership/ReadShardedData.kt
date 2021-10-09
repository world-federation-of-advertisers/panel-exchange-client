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

package org.wfanet.panelmatch.client.privatemembership

import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.apache.beam.sdk.transforms.Create
import org.apache.beam.sdk.transforms.PTransform
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PBegin
import org.apache.beam.sdk.values.PCollection
import org.wfanet.panelmatch.client.storage.StorageFactory
import org.wfanet.panelmatch.client.storage.toByteString
import org.wfanet.panelmatch.common.ShardedFileName
import org.wfanet.panelmatch.common.beam.groupByKey
import org.wfanet.panelmatch.common.beam.kvOf
import org.wfanet.panelmatch.common.beam.parDo

/** Reads each file mentioned in [fileSpec] as a [ByteString]. */
class ReadShardedData(private val fileSpec: String, private val storageFactory: StorageFactory) :
  PTransform<PBegin, PCollection<ByteString>>() {
  override fun expand(input: PBegin): PCollection<ByteString> {
    val fileNames: PCollection<KV<Int, String>> =
      input.pipeline.apply("Create FileSpec", Create.of(fileSpec)).parDo {
        val shardedFileName = ShardedFileName(fileSpec)
        for (i in 0 until shardedFileName.shardCount) {
          yield(kvOf(i, shardedFileName.fileNameForShard(i)))
        }
      }

    // GroupByKey prevents fusing `mapValues` since the previous ParDo has high fan-out.
    return fileNames.groupByKey("Prevent fusion").parDo { kv ->
      val bytes =
        runBlocking(Dispatchers.IO) {
          storageFactory.build().getBlob(kv.value.single())?.toByteString()
        }

      if (bytes != null) {
        yield(bytes)
      }
    }
  }
}
