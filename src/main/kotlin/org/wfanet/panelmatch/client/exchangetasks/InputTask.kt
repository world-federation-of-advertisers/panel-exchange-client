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
import java.time.Duration
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.toList
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.panelmatch.client.storage.Storage.STORAGE_TYPE
import org.wfanet.panelmatch.client.storage.batchRead

/**
 * Input task waits for output labels to be present. Clients should not pass in the actual required
 * inputs for the next task. Instead, these outputs should be small files with contents of `done`
 * that are written after the actual outputs are done being written.
 */
class InputTask(
  val exchangeKey: String,
  val step: ExchangeWorkflow.Step,
  val retryDuration: Duration
) : ExchangeTask {

  override suspend fun execute(input: Map<String, ByteString>): Map<String, ByteString> {
    val privateOutputLabels = step.getPrivateOutputLabelsMap()
    val sharedOutputLabels = step.getSharedOutputLabelsMap()
    flow<String> {
        batchRead(
          storageType = STORAGE_TYPE.PRIVATE,
          exchangeKey = exchangeKey,
          step = step,
          inputLabels = privateOutputLabels
        )
          .keys
          .forEach { emit(it) }
        batchRead(
          storageType = STORAGE_TYPE.SHARED,
          exchangeKey = exchangeKey,
          step = step,
          inputLabels = sharedOutputLabels
        )
          .keys
          .forEach { emit(it) }
      }
      .debounce(retryDuration.toMillis())
      .retryWhen { cause, attempt -> true }
      .toList()
    // This function only returns that input is ready. It does not return actual values.
    return emptyMap<String, ByteString>()
  }
}
