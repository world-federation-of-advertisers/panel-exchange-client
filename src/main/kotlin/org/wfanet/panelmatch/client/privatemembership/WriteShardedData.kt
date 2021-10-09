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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.transforms.DoFn
import org.apache.beam.sdk.transforms.PTransform
import org.apache.beam.sdk.transforms.ParDo
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.apache.beam.sdk.values.PCollectionView
import org.apache.beam.sdk.values.PInput
import org.apache.beam.sdk.values.POutput
import org.apache.beam.sdk.values.PValue
import org.apache.beam.sdk.values.TupleTag
import org.wfanet.panelmatch.client.storage.StorageFactory
import org.wfanet.panelmatch.common.ShardedFileName
import org.wfanet.panelmatch.common.beam.keys
import org.wfanet.panelmatch.common.beam.toListView

/**
 * Writes the value of each input KV to the file in [fileSpec] specified by the key.
 *
 * For example, a KV `(123, someBytes)` with fileSpec "foo-*-of-456" will have `someBytes` written
 * to file "foo-123-of-456".
 */
class WriteShardedData(private val fileSpec: String, private val storageFactory: StorageFactory) :
  PTransform<PCollection<KV<Int, ByteString>>, WriteShardedData.WriteResult>() {

  /** [POutput] holding filenames written. */
  class WriteResult(private val fileNames: PCollection<String>) : POutput {
    override fun getPipeline(): Pipeline = fileNames.pipeline

    override fun expand(): Map<TupleTag<*>, PValue> {
      return mapOf(tag to fileNames)
    }

    override fun finishSpecifyingOutput(
      transformName: String,
      input: PInput,
      transform: PTransform<*, *>
    ) {}

    companion object {
      private val tag = TupleTag<String>()
    }
  }

  override fun expand(input: PCollection<KV<Int, ByteString>>): WriteResult {
    val shardIdsView = input.keys().toListView("List All Shards")
    val filesWritten =
      input.apply(
        "Write $fileSpec",
        ParDo.of(WriteFilesFn(fileSpec, storageFactory, shardIdsView)).withSideInputs(shardIdsView)
      )

    return WriteResult(filesWritten)
  }
}

private class WriteFilesFn(
  private val fileSpec: String,
  private val storageFactory: StorageFactory,
  private val allShardsView: PCollectionView<List<Int>>
) : DoFn<KV<Int, ByteString>, String>() {

  private var shardsValidated: Boolean = false

  @ProcessElement
  fun processElement(context: ProcessContext) {
    val shardedFileName = ShardedFileName(fileSpec)
    val kv = context.element()
    val shard = kv.key
    val blobContents = kv.value

    // Sanity check to ensure all shards are present:
    if (!shardsValidated) {
      val allShards = context.sideInput(allShardsView)
      require(allShards.size == allShards.toSet().size) { "Shards are not distinct: $allShards" }
      require(allShards.all { it in 0 until shardedFileName.shardCount }) {
        "Invalid shards: $allShards"
      }
      shardsValidated = true
    }

    val blobKey = shardedFileName.fileNameForShard(shard)
    val storageClient = storageFactory.build()

    runBlocking(Dispatchers.IO) {
      storageClient.getBlob(blobKey)?.delete()
      storageClient.createBlob(blobKey, flowOf(blobContents))
    }
  }
}
