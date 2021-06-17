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
  enum class STORAGE_CLASS {
    PRIVATE,
    SHARED
  }
  enum class STORAGE_TYPE {
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
}

/**
 * Returns a storage and path given a storage type, exchangeKey, label, and step.
 *
 * TODO: This should be extended to read from a local config along with step meta data like the data
 * label, input labels, and output labels and return a StorageClient such as FileSystem, GCS, S3,
 * etc
 */
private suspend fun getStorageAndPathForStep(
  storageClass: Storage.STORAGE_CLASS,
  exchangeKey: String,
  label: String,
  step: ExchangeWorkflow.Step
): Pair<Storage, String> {
  val storageType: Storage.STORAGE_TYPE =
    Storage.STORAGE_TYPE.valueOf(System.getProperty("${storageClass.toString()}_STORAGE_TYPE"))
  val storage: Storage =
    when (storageType) {
      Storage.STORAGE_TYPE.FILESYSTEM ->
        FileSystemStorage(
          baseDir = requireNotNull(System.getProperty("${storageClass.toString()}_FILESYSTEM_PATH"))
        )
      Storage.STORAGE_TYPE.IN_MEMORY ->
        InMemoryStorage(
          keyPrefix =
            requireNotNull(System.getProperty("${storageClass.toString()}_INMEMORY_PREFIX"))
        )
    }
  val fields: List<String> = listOf(exchangeKey, label)
  val path = fields.joinToString(separator = "-")
  return Pair(storage, path)
}

/**
 * Reads different data from a [Storage.STORAGE_TYPE] based on [exchangeKey] and [ExchangeStep] and
 * returns Map<String, ByteString> of different input
 */
suspend fun batchRead(
  storageClass: Storage.STORAGE_CLASS,
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
                storageClass = storageClass,
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
 * Writes output [data] for [storageClass] to different storage based on [exchangeKey] and
 * [ExchangeStep].
 */
suspend fun batchWrite(
  storageClass: Storage.STORAGE_CLASS,
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
              storageClass = storageClass,
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
 * Waits for all private and shared task input from different storage based on [exchangeKey] and
 * [ExchangeStep] and returns when ready
 */
suspend fun waitForOutputsToBeReady(exchangeKey: String, step: ExchangeWorkflow.Step) =
    coroutineScope {
  val privateOutputLabels = step.getPrivateOutputLabelsMap()
  val sharedOutputLabels = step.getSharedOutputLabelsMap()
  val readDeferreds: List<Deferred<Map<String, ByteString>>> =
    listOf(
      async(start = CoroutineStart.DEFAULT) {
        batchRead(
          storageClass = Storage.STORAGE_CLASS.PRIVATE,
          exchangeKey = exchangeKey,
          step = step,
          inputLabels = privateOutputLabels
        )
      },
      async(start = CoroutineStart.DEFAULT) {
        batchRead(
          storageClass = Storage.STORAGE_CLASS.SHARED,
          exchangeKey = exchangeKey,
          step = step,
          inputLabels = sharedOutputLabels
        )
      }
    )
  readDeferreds.awaitAll()
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
        storageClass = Storage.STORAGE_CLASS.PRIVATE,
        exchangeKey = exchangeKey,
        step = step,
        inputLabels = privateInputLabels
      )
    }
  val taskSharedInput =
    async(start = CoroutineStart.DEFAULT) {
      batchRead(
        storageClass = Storage.STORAGE_CLASS.SHARED,
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
      storageClass = Storage.STORAGE_CLASS.PRIVATE,
      exchangeKey = exchangeKey,
      step = step,
      outputLabels = privateOutputLabels,
      data = taskOutput
    )
  }
  async {
    batchWrite(
      storageClass = Storage.STORAGE_CLASS.SHARED,
      exchangeKey = exchangeKey,
      step = step,
      outputLabels = sharedOutputLabels,
      data = taskOutput
    )
  }
}
