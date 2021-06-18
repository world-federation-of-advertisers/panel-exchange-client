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

import com.google.protobuf.ByteString
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow

/** Interface for Storage adapter. */
interface Storage {
  enum class StorageClass {
    PRIVATE,
    SHARED
  }
  enum class StorageType {
    FILESYSTEM,
    IN_MEMORY
  }

  /**
   * Reads input data from given path.
   *
   * @param path String location of input data to read from.
   * @return Input data.
   */
  suspend fun read(path: String): ByteString

  /**
   * Writes output data into given path.
   *
   * @param path String location of data to write to.
   */
  suspend fun write(path: String, data: ByteString)

  /** Reads different data from [storage] and returns Map<String, ByteString> of different input */
  suspend fun batchRead(inputLabels: Map<String, String>): Map<String, ByteString> =
    withContext(Dispatchers.IO) {
      coroutineScope {
        inputLabels
          .mapValues { entry -> async(start = CoroutineStart.DEFAULT) { read(path = entry.value) } }
          .mapValues { entry -> entry.value.await() }
      }
    }

  /** Writes output [data] to [storage] based on [outputLabels] */
  suspend fun batchWrite(outputLabels: Map<String, String>, data: Map<String, ByteString>) =
    withContext(Dispatchers.IO) {
      coroutineScope {
        for ((key, value) in outputLabels) {
          async { write(path = value, data = requireNotNull(data[key])) }
        }
      }
    }
}

/**
 * Waits for all private and shared task input from different storage based on [exchangeKey] and
 * [ExchangeStep] and returns when ready
 */
suspend fun waitForOutputsToBeReady(
  preferredSharedStorage: Storage,
  preferredPrivateStorage: Storage,
  step: ExchangeWorkflow.Step
) = coroutineScope {
  val privateOutputLabels = step.getPrivateOutputLabelsMap()
  val sharedOutputLabels = step.getSharedOutputLabelsMap()
  val readDeferreds: List<Deferred<Map<String, ByteString>>> =
    listOf(
      async(start = CoroutineStart.DEFAULT) {
        preferredPrivateStorage.batchRead(inputLabels = privateOutputLabels)
      },
      async(start = CoroutineStart.DEFAULT) {
        preferredSharedStorage.batchRead(inputLabels = sharedOutputLabels)
      }
    )
  readDeferreds.awaitAll()
}

/**
 * Reads private and shared task input from different storage based on [exchangeKey] and
 * [ExchangeStep] and returns Map<String, ByteString> of different input
 */
suspend fun getAllInputForStep(
  preferredSharedStorage: Storage,
  preferredPrivateStorage: Storage,
  step: ExchangeWorkflow.Step
): Map<String, ByteString> = coroutineScope {
  val privateInputLabels = step.getPrivateInputLabelsMap()
  val sharedInputLabels = step.getSharedInputLabelsMap()
  val taskPrivateInput =
    async(start = CoroutineStart.DEFAULT) {
      preferredPrivateStorage.batchRead(inputLabels = privateInputLabels)
    }
  val taskSharedInput =
    async(start = CoroutineStart.DEFAULT) {
      preferredSharedStorage.batchRead(inputLabels = sharedInputLabels)
    }
  taskPrivateInput.await().toMutableMap().apply { putAll(taskSharedInput.await()) }
}

/**
 * Writes private and shared task output [taskOutput] to different storage based on [exchangeKey]
 * and [ExchangeStep].
 */
suspend fun writeAllOutputForStep(
  preferredSharedStorage: Storage,
  preferredPrivateStorage: Storage,
  step: ExchangeWorkflow.Step,
  taskOutput: Map<String, ByteString>
) = coroutineScope {
  val privateOutputLabels = step.getPrivateOutputLabelsMap()
  val sharedOutputLabels = step.getSharedOutputLabelsMap()
  async {
    preferredPrivateStorage.batchWrite(outputLabels = privateOutputLabels, data = taskOutput)
  }
  async { preferredSharedStorage.batchWrite(outputLabels = sharedOutputLabels, data = taskOutput) }
}
