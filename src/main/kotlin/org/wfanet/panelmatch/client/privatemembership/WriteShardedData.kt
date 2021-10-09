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
import org.wfanet.panelmatch.common.beam.count
import org.wfanet.panelmatch.common.beam.groupByKey

class WriteShardedData(private val fileSpec: String, private val storageFactory: StorageFactory) :
  PTransform<PCollection<KV<ShardId, ByteString>>, WriteShardedData.WriteResult>() {

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

  override fun expand(input: PCollection<KV<ShardId, ByteString>>): WriteResult {
    val shardCountView: PCollectionView<Long> = input.count("Count Shards")
    val filesWritten =
      input
        .groupByKey("Group by Shard")
        .apply(
          "Write Files",
          ParDo.of(WriteFilesFn(fileSpec, storageFactory, shardCountView))
            .withSideInputs(shardCountView)
        )

    return WriteResult(filesWritten)
  }
}

private class WriteFilesFn(
  private val fileSpec: String,
  private val storageFactory: StorageFactory,
  private val shardCount: PCollectionView<Long>
) : DoFn<KV<ShardId, Iterable<ByteString>>, String>() {

  @ProcessElement
  fun processElement(context: ProcessContext) {
    val shardedFileName = ShardedFileName(fileSpec)
    val kv = context.element()
    val shardId = kv.key.id
    val blobContents = kv.value.single()

    // Sanity check to ensure all shards are present:
    val actualShardCount = context.sideInput(shardCount)
    val expectedShardCount = shardedFileName.shardCount.toLong()
    require(expectedShardCount == actualShardCount) {
      "Expected $expectedShardCount shards but got $actualShardCount"
    }
    require(shardId in 0 until expectedShardCount) {
      "Invalid shard id: $shardId is not within [0, $expectedShardCount)"
    }

    val blobKey = shardedFileName.fileNameForShard(shardId)
    val storageClient = storageFactory.build()

    runBlocking(Dispatchers.IO) {
      storageClient.getBlob(blobKey)?.delete()
      storageClient.createBlob(blobKey, flowOf(blobContents))
    }
  }
}
