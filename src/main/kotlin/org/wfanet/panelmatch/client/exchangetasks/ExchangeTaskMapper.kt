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
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.panelmatch.client.storage.Storage

/**
 * Maps ExchangeWorkflow.Step to respective tasks. Retrieves necessary inputs. Executes step. Stores
 * outputs.
 *
 * @param ExchangeWorkflow.Step to execute.
 * @param input inputs needed by all [task]s.
 * @param storage the Storage class to store the intermediary steps
 * @param sendDebugLog function which writes logs happened during execution.
 * @return mapped output. This is either a ByteString or null for input steps.
 */
class ExchangeTaskMapper {

  suspend fun execute(
    step: ExchangeWorkflow.Step,
    input: Map<String, ByteString>,
    storage: Storage,
    sendDebugLog: suspend (String) -> Unit
  ): ByteString? {
    val inputLabels = step.getInputLabelsMap()
    val outputLabels = step.getOutputLabelsMap()
    val outputFieldName = requireNotNull(outputLabels["output"])
    when (step.getStepCase()) {
      ExchangeWorkflow.Step.StepCase.INPUT -> {
        val inputFieldName = requireNotNull(inputLabels["input"])
        storage.write(outputFieldName, requireNotNull(input[inputFieldName]))
        return null
      }
      // TODO split this up into encrypt and reencrypt
      ExchangeWorkflow.Step.StepCase.ENCRYPT_AND_SHARE -> {
        when (step.encryptAndShare.getInputFormat()) {
          ExchangeWorkflow.Step.EncryptAndShareStep.InputFormat.PLAINTEXT -> {
            val taskInput: Map<String, ByteString> =
              mapOf(
                "encryption-key" to storage.read(requireNotNull(inputLabels["crypto-key"])),
                "unencrypted-data" to storage.read(requireNotNull(inputLabels["unencrypted-data"]))
              )
            val taskOutput = EncryptTask().execute(taskInput, sendDebugLog)
            val encryptedData = requireNotNull(taskOutput["encrypted-data"])
            // TODO store data in shared location or send to Measurement Coordinator
            storage.write(outputFieldName, encryptedData)
            return encryptedData
          }
          ExchangeWorkflow.Step.EncryptAndShareStep.InputFormat.CIPHERTEXT -> {
            val taskInput: Map<String, ByteString> =
              mapOf(
                "encryption-key" to storage.read(requireNotNull(inputLabels["crypto-key"])),
                "encrypted-data" to storage.read(requireNotNull(inputLabels["encrypted-data"]))
              )
            val taskOutput = ReEncryptTask().execute(taskInput, sendDebugLog)
            val reencryptedData = requireNotNull(taskOutput["reencrypted-data"])
            // TODO store data in shared location or send to Measurement Coordinator
            storage.write(outputFieldName, reencryptedData)
            return reencryptedData
          }
          else -> {
            error("Unsupported encryption type")
          }
        }
      }
      ExchangeWorkflow.Step.StepCase.DECRYPT -> {
        val taskInput: Map<String, ByteString> =
          mapOf(
            "encryption-key" to storage.read(requireNotNull(inputLabels["crypto-key"])),
            "encrypted-data" to storage.read(requireNotNull(inputLabels["encrypted-data"]))
          )
        val taskOutput = ReEncryptTask().execute(taskInput, sendDebugLog)
        val decryptedData = requireNotNull(taskOutput["reencrypted-data"])
        // TODO store data in shared location or send to Measurement Coordinator
        storage.write(outputFieldName, decryptedData)
        return decryptedData
      }
      else -> {
        error("Unsupported step type")
      }
    }
  }
}
