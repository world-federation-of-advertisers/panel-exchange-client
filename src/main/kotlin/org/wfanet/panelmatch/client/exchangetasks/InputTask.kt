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
import java.lang.IllegalArgumentException
import kotlinx.coroutines.flow.Flow
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.measurement.common.throttler.Throttler
import org.wfanet.panelmatch.client.storage.StorageNotFoundException
import org.wfanet.panelmatch.client.storage.VerifiedStorageClient

/**
 * Input task waits for input labels to be present. Clients should not pass in the actual required
 * inputs for the next task. Instead, these inputs should be small files with contents of `done`
 * that are written after the actual inputs are done being written.
 */
class InputTask(private val throttler: Throttler) : ExchangeTask {

  private lateinit var step: ExchangeWorkflow.Step
  private lateinit var storage: VerifiedStorageClient

  /** Reads a single blob from [storage] as specified in [step]. */
  private suspend fun readValue() {
    storage.verifiedBatchRead(inputLabels = step.inputLabelsMap)
  }

  private suspend fun isReady(): Boolean {
    return try {
      readValue()
      true
    } catch (e: StorageNotFoundException) {
      false
    }
  }

  override suspend fun executeWithStorage(
    input: Map<String, VerifiedStorageClient.VerifiedBlob>,
    storage: VerifiedStorageClient,
    step: ExchangeWorkflow.Step
  ): Map<String, Flow<ByteString>> {
    require(step.inputLabelsCount == 1)
    require(step.outputLabelsCount == 0)
    this.storage = storage
    this.step = step

    while (true) {
      if (throttler.onReady { isReady() }) {
        // This function only returns that input is ready. It does not return actual values.
        return emptyMap()
      }
    }
  }

  override suspend fun execute(
    input: Map<String, VerifiedStorageClient.VerifiedBlob>
  ): Map<String, Flow<ByteString>> {
    throw IllegalArgumentException("InputTask does not support execute.")
  }
}
