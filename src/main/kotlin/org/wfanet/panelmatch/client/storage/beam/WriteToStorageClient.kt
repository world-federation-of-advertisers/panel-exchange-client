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

import com.google.protobuf.ByteString
import java.io.Serializable
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.runBlocking
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.transforms.DoFn
import org.apache.beam.sdk.transforms.PTransform
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.apache.beam.sdk.values.PInput
import org.apache.beam.sdk.values.POutput
import org.apache.beam.sdk.values.PValue
import org.apache.beam.sdk.values.TupleTag
import org.wfanet.measurement.storage.StorageClient
import org.wfanet.panelmatch.client.storage.beam.WriteToStorageClient.Parameters
import org.wfanet.panelmatch.client.storage.beam.WriteToStorageClient.StorageClientProvider
import org.wfanet.panelmatch.client.storage.beam.WriteToStorageClient.WriteResult
import org.wfanet.panelmatch.common.beam.groupByKey
import org.wfanet.panelmatch.common.beam.keyBy
import org.wfanet.panelmatch.common.beam.parDo

/** [PTransform] to write a [PCollection] to a [StorageClient]. */
class WriteToStorageClient(
  private val storageClientProvider: StorageClientProvider,
  private val parameters: Parameters
) : PTransform<PCollection<ByteString>, WriteResult>() {
  interface StorageClientProvider : Serializable {
    fun get(): StorageClient
  }

  data class Parameters(val numShards: Int, val prefix: String) : Serializable

  class WriteResult(val blobKeys: PCollection<String>) : POutput {
    private val writesTag = TupleTag<ByteString>("successful-writes")

    override fun getPipeline(): Pipeline {
      return blobKeys.pipeline
    }

    override fun expand(): Map<TupleTag<*>, PValue> {
      return mapOf(writesTag to blobKeys)
    }

    override fun finishSpecifyingOutput(
      transformName: String,
      input: PInput,
      transform: PTransform<*, *>
    ) {}
  }

  override fun expand(input: PCollection<ByteString>): WriteResult {
    val blobKeys: PCollection<String> =
      input
        .keyBy(name = "Add Random Keys") { Random.nextInt(parameters.numShards) }
        .groupByKey(name = "Group by Random Keys")
        .parDo(StorageWriterDoFn(storageClientProvider, parameters))

    return WriteResult(blobKeys)
  }
}

private class StorageWriterDoFn(
  private val storageClientProvider: StorageClientProvider,
  private val parameters: Parameters
) : DoFn<KV<Int, Iterable<@JvmWildcard ByteString>>, String>() {
  lateinit var storageClient: StorageClient

  @Setup
  fun setup() {
    storageClient = storageClientProvider.get()
  }

  @ProcessElement
  fun processElement(
    @Element kv: KV<Int, Iterable<@JvmWildcard ByteString>>,
    out: OutputReceiver<String>
  ) {
    val shard = kv.key
    val shardString = "%05d".format(shard)
    val shardCountString = "%05d".format(parameters.numShards)
    val blobKey = "${parameters.prefix}-$shardString-of-$shardCountString"
    writeToStorage(blobKey, kv.value.asFlow())
    out.output(blobKey)
  }

  private fun writeToStorage(blobKey: String, data: Flow<ByteString>) {
    runBlocking(Dispatchers.IO) {
      // TODO: check if blob exists first, if it does, then what? Delete and rewrite?
      // TODO: instead of concatenating all the values, pack into a proto and sign.
      storageClient.createBlob(blobKey, data)
    }
  }
}
