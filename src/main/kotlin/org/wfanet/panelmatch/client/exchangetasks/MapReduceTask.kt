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

package org.wfanet.panelmatch.client.exchangetasks

import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.toByteStringUtf8
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.apache.beam.sdk.PipelineResult
import org.wfanet.measurement.storage.StorageClient.Blob
import org.wfanet.panelmatch.client.storage.StorageFactory
import org.wfanet.panelmatch.common.ShardedFileName

/** Executes Exchange Steps that rely on Apache Beam. */
class MapReduceTask(
  private val mapReduceRunner: MapReduceRunner,
  private val storageFactory: StorageFactory,
  private val inputLabels: Map<String, String>,
  private val outputManifests: Map<String, ShardedFileName>,
  private val executeOnPipeline: suspend MapReduceContext.() -> Unit
) : ExchangeTask {
  override suspend fun execute(input: Map<String, Blob>): Map<String, Flow<ByteString>> {
    val context =
      MapReduceContext(
        mapReduceRunner.pipeline,
        outputManifests,
        inputLabels,
        input,
        storageFactory
      )
    context.executeOnPipeline()

    val finalState = mapReduceRunner.run().waitUntilFinish()
    check(finalState == PipelineResult.State.DONE) { "MapReduce is in state $finalState" }

    return outputManifests.mapValues { flowOf(it.value.spec.toByteStringUtf8()) }
  }
}
