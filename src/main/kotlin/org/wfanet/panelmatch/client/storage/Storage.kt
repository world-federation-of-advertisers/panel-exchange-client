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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow

/** Interface for Storage adapter. */
interface Storage {
  enum class STORAGE_TYPE {
    PRIVATE,
    SHARED
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
}

/**
 * Returns a storage and path given a storage type, exchangeKey, label, and step.
 *
 * TODO: This should be extended to read from a local config along with step meta data like the data
 * label, input labels, and output labels and return a StorageClient such as FileSystem, GCS, S3,
 * etc
 */
private suspend fun getStorageAndPathForStep(
  storageType: Storage.STORAGE_TYPE,
  exchangeKey: String,
  label: String,
  step: ExchangeWorkflow.Step
): Pair<Storage, String> {
  val storage = FileSystemStorage(storageType = storageType, label = label)
  val fields: List<String> = listOf(exchangeKey, label)
  val path = fields.joinToString(separator = "-")
  return Pair(storage, path)
}

/**
 * Reads different data from a [Storage.STORAGE_TYPE] based on [exchangeKey] and [ExchangeStep] and
 * returns Map<String, ByteString> of different input
 */
suspend fun batchRead(
  storageType: Storage.STORAGE_TYPE,
  exchangeKey: String,
  step: ExchangeWorkflow.Step,
  inputLabels: Map<String, String>
): Map<String, ByteString> =
  withContext(Dispatchers.IO) {
    coroutineScope {
      inputLabels
        .mapValues { entry ->
          async(start = CoroutineStart.DEFAULT) {
            val (storage: Storage, path: String) =
              getStorageAndPathForStep(
                storageType = storageType,
                exchangeKey = exchangeKey,
                label = entry.value,
                step = step
              )
            storage.read(path = path)
          }
        }
        .mapValues { entry -> entry.value.await() }
    }
  }

/**
 * Writes output [data] for [storageType] to different storage based on [exchangeKey] and
 * [ExchangeStep].
 */
suspend fun batchWrite(
  storageType: Storage.STORAGE_TYPE,
  exchangeKey: String,
  step: ExchangeWorkflow.Step,
  outputLabels: Map<String, String>,
  data: Map<String, ByteString>
) =
  withContext(Dispatchers.IO) {
    coroutineScope {
      for ((key, value) in outputLabels) {
        async {
          val (storage: Storage, path: String) =
            getStorageAndPathForStep(
              storageType = storageType,
              exchangeKey = exchangeKey,
              label = value,
              step = step
            )
          storage.write(path = path, data = requireNotNull(data[key]))
        }
      }
    }
  }

/**
 * Reads private and shared task input from different storage based on [exchangeKey] and
 * [ExchangeStep] and returns Map<String, ByteString> of different input
 */
suspend fun getAllInputForStep(
  exchangeKey: String,
  step: ExchangeWorkflow.Step
): Map<String, ByteString> = coroutineScope {
  val privateInputLabels = step.getPrivateInputLabelsMap()
  val sharedInputLabels = step.getSharedInputLabelsMap()
  val taskPrivateInput =
    async(start = CoroutineStart.DEFAULT) {
      batchRead(
        storageType = Storage.STORAGE_TYPE.PRIVATE,
        exchangeKey = exchangeKey,
        step = step,
        inputLabels = privateInputLabels
      )
    }
  val taskSharedInput =
    async(start = CoroutineStart.DEFAULT) {
      batchRead(
        storageType = Storage.STORAGE_TYPE.SHARED,
        exchangeKey = exchangeKey,
        step = step,
        inputLabels = sharedInputLabels
      )
    }
  taskPrivateInput.await().toMutableMap().apply { putAll(taskSharedInput.await()) }
}

/**
 * Writes private and shared task output [taskOutput] to different storage based on [exchangeKey]
 * and [ExchangeStep].
 */
suspend fun writeAllOutputForStep(
  exchangeKey: String,
  step: ExchangeWorkflow.Step,
  taskOutput: Map<String, ByteString>
) = coroutineScope {
  val privateOutputLabels = step.getPrivateOutputLabelsMap()
  val sharedOutputLabels = step.getSharedOutputLabelsMap()
  async {
    batchWrite(
      storageType = Storage.STORAGE_TYPE.PRIVATE,
      exchangeKey = exchangeKey,
      step = step,
      outputLabels = privateOutputLabels,
      data = taskOutput
    )
  }
  async {
    batchWrite(
      storageType = Storage.STORAGE_TYPE.SHARED,
      exchangeKey = exchangeKey,
      step = step,
      outputLabels = sharedOutputLabels,
      data = taskOutput
    )
  }
}
