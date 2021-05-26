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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.panelmatch.client.storage.Storage
import org.wfanet.panelmatch.protocol.common.JniDeterministicCommutativeEncryption

/**
 * Maps ExchangeWorkflow.Step to respective tasks. Retrieves necessary inputs. Executes step. Stores
 * outputs.
 *
 * @param ExchangeWorkflow.Step to execute.
 * @param input inputs needed by all [task]s.
 * @param storage the Storage class to store the intermediary steps
 * @param sendDebugLog function which writes logs happened during execution.
 * @return mapped output.
 */
class ExchangeTaskMapper {
  private val deterministicCommutativeEncryption = JniDeterministicCommutativeEncryption()

  private suspend fun mapToTask(
    step: ExchangeWorkflow.Step,
    taskInput: Map<String, ByteString>,
    sendDebugLog: suspend (String) -> Unit
  ): Map<String, ByteString> {
    when (step.getStepCase()) {
      // TODO split this up into encrypt and reencrypt
      ExchangeWorkflow.Step.StepCase.ENCRYPT_AND_SHARE -> {
        when (step.encryptAndShare.getInputFormat()) {
          ExchangeWorkflow.Step.EncryptAndShareStep.InputFormat.PLAINTEXT -> {
            return EncryptionExchangeTask.forEncryption(JniDeterministicCommutativeEncryption())
              .execute(taskInput, sendDebugLog)
          }
          ExchangeWorkflow.Step.EncryptAndShareStep.InputFormat.CIPHERTEXT -> {
            return EncryptionExchangeTask.forReEncryption(JniDeterministicCommutativeEncryption())
              .execute(taskInput, sendDebugLog)
          }
          else -> {
            error("Unsupported encryption type")
          }
        }
      }
      ExchangeWorkflow.Step.StepCase.DECRYPT -> {
        return EncryptionExchangeTask.forDecryption(JniDeterministicCommutativeEncryption())
          .execute(taskInput, sendDebugLog)
      }
      else -> {
        error("Unsupported step type")
      }
    }
  }

  suspend fun execute(
    step: ExchangeWorkflow.Step,
    input: Map<String, ByteString>,
    storage: Storage,
    sendDebugLog: suspend (String) -> Unit
  ): Map<String, ByteString> {
    val inputLabels = step.getInputLabelsMap()
    val outputLabels = step.getOutputLabelsMap()
    if (step.getStepCase() == ExchangeWorkflow.Step.StepCase.INPUT) {
      val inputFieldName = requireNotNull(inputLabels["input"])
      val outputFieldName = requireNotNull(outputLabels["output"])
      storage.write(outputFieldName, requireNotNull(input[inputFieldName]))
      return emptyMap<String, ByteString>()
    }
    val taskInput: Map<String, ByteString> = coroutineScope {
      inputLabels.mapValues { entry -> async { storage.read(entry.value) } }.mapValues { entry ->
        entry.value.await()
      }
    }
    val taskOutput: Map<String, ByteString> = mapToTask(step, taskInput, sendDebugLog)
    coroutineScope {
      for ((key, value) in outputLabels) {
        launch { storage.write(value, requireNotNull(taskOutput[key])) }
      }
    }
    // TODO make rpc call to update that task was completed. Also catch if task errored and report
    // that as well.
    return taskOutput
  }
}
