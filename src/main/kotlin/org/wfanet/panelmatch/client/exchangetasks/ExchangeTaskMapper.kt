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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.panelmatch.client.logger.loggerFor
import org.wfanet.panelmatch.client.storage.Storage.STORAGE_TYPE
import org.wfanet.panelmatch.client.storage.batchRead
import org.wfanet.panelmatch.client.storage.batchWrite
import org.wfanet.panelmatch.protocol.common.Cryptor
import org.wfanet.panelmatch.protocol.common.JniDeterministicCommutativeCryptor

/**
 * Maps ExchangeWorkflow.Step to respective tasks. Retrieves necessary inputs. Executes step. Stores
 * outputs.
 *
 * @param ExchangeWorkflow.Step to execute.
 * @param input inputs needed by all [task]s.
 * @param storage the Storage class to store the intermediary steps
 * @return mapped output.
 */
class ExchangeTaskMapper(
  private val deterministicCommutativeCryptor: Cryptor = JniDeterministicCommutativeCryptor()
) {
  private val LOGGER = loggerFor(javaClass)

  private suspend fun getExchangeTaskForStep(step: ExchangeWorkflow.Step): ExchangeTask {
    return when (step.getStepCase()) {
      ExchangeWorkflow.Step.StepCase.ENCRYPT_STEP ->
        CryptorExchangeTask.forEncryption(deterministicCommutativeCryptor)
      ExchangeWorkflow.Step.StepCase.REENCRYPT_STEP ->
        CryptorExchangeTask.forReEncryption(deterministicCommutativeCryptor)
      ExchangeWorkflow.Step.StepCase.DECRYPT_STEP ->
        CryptorExchangeTask.forDecryption(deterministicCommutativeCryptor)
      else -> error("Unsupported step type")
    }
  }

  suspend fun execute(exchangeKey: String, step: ExchangeWorkflow.Step) {
    LOGGER.info("Execute step: ${step.toString()}")
    val privateInputLabels = step.getPrivateInputLabelsMap()
    val privateOutputLabels = step.getPrivateOutputLabelsMap()
    val sharedInputLabels = step.getSharedInputLabelsMap()
    val sharedOutputLabels = step.getSharedOutputLabelsMap()
    if (step.getStepCase() == ExchangeWorkflow.Step.StepCase.INPUT_STEP) {
      do {
        try {
          batchRead(
            storageType = STORAGE_TYPE.PRIVATE,
            exchangeKey = exchangeKey,
            step = step,
            inputLabels = mapOf("input" to "mp-crypto-key")
          )
          return
        } catch (e: IllegalArgumentException) {
          delay(500)
        }
      } while (true)
    }
    coroutineScope {
      val taskPrivateInput =
        async(start = CoroutineStart.DEFAULT) {
          batchRead(
            storageType = STORAGE_TYPE.PRIVATE,
            exchangeKey = exchangeKey,
            step = step,
            inputLabels = privateInputLabels
          )
        }
      val taskSharedInput =
        async(start = CoroutineStart.DEFAULT) {
          batchRead(
            storageType = STORAGE_TYPE.SHARED,
            exchangeKey = exchangeKey,
            step = step,
            inputLabels = sharedInputLabels
          )
        }
      val taskInput: Map<String, ByteString> =
        LinkedHashMap(taskPrivateInput.await()).apply { putAll(taskSharedInput.await()) }
      val taskOutput: Map<String, ByteString> = getExchangeTaskForStep(step).execute(taskInput)
      coroutineScope {
        launch {
          batchWrite(
            storageType = STORAGE_TYPE.PRIVATE,
            exchangeKey = exchangeKey,
            step = step,
            outputLabels = privateOutputLabels,
            data = taskOutput
          )
        }
        launch {
          batchWrite(
            storageType = STORAGE_TYPE.SHARED,
            exchangeKey = exchangeKey,
            step = step,
            outputLabels = sharedOutputLabels,
            data = taskOutput
          )
        }
      }
    }
    return
  }
}
