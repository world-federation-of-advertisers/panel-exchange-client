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

package org.wfanet.panelmatch.client.launcher

import com.google.protobuf.ByteString
import java.security.PrivateKey
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.wfanet.measurement.api.v2alpha.ExchangeStep
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttempt
import org.wfanet.measurement.api.v2alpha.ExchangeStepAttemptKey
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.panelmatch.client.exchangetasks.ExchangeTaskDetails
import org.wfanet.panelmatch.client.exchangetasks.ExchangeTaskMapper
import org.wfanet.panelmatch.client.exchangetasks.ExchangeTaskStorageType
import org.wfanet.panelmatch.client.logger.addToTaskLog
import org.wfanet.panelmatch.client.logger.getAndClearTaskLog
import org.wfanet.panelmatch.client.logger.loggerFor
import org.wfanet.panelmatch.client.storage.StorageSelector
import org.wfanet.panelmatch.client.storage.VerifiedStorageClient
import org.wfanet.panelmatch.client.storage.VerifiedStorageClient.VerifiedBlob
import org.wfanet.panelmatch.common.CertificateMapper
import org.wfanet.panelmatch.common.Timeout

/**
 * Maps ExchangeWorkflow.Step to respective tasks. Retrieves necessary inputs. Executes step. Stores
 * outputs.
 */
class ExchangeTaskExecutor(
  private val apiClient: ApiClient,
  private val certificateMapper: CertificateMapper,
  private val timeout: Timeout,
  private val storageSelector: StorageSelector,
  private val exchangeTaskMapper: ExchangeTaskMapper,
  private val privateKey: PrivateKey
) : ExchangeStepExecutor {

  private lateinit var privateStorage: VerifiedStorageClient

  /**
   * Reads inputs for [step], executes [step], and writes the outputs to appropriate
   * [VerifiedStorageClient].
   */
  override suspend fun execute(attemptKey: ExchangeStepAttemptKey, exchangeStep: ExchangeStep) {
    withContext(CoroutineName(attemptKey.exchangeStepAttemptId)) {
      try {
        val workflow =
          ExchangeWorkflow.parseFrom(exchangeStep.signedExchangeWorkflow.serializedExchangeWorkflow)
        val wfStep = workflow.getSteps(exchangeStep.stepIndex)

        tryExecute(attemptKey, wfStep, workflow.exchangeIdentifiers)
      } catch (e: Exception) {
        logger.addToTaskLog(e.toString())
        markAsFinished(attemptKey, ExchangeStepAttempt.State.FAILED)
        throw e
      }
    }
  }

  private suspend fun readInputs(inputLabelsMap: Map<String, String>): Map<String, VerifiedBlob> {
    return privateStorage.verifiedBatchRead(inputLabels = inputLabelsMap)
  }

  private suspend fun writeOutputs(
    outputLabelsMap: Map<String, String>,
    taskOutput: Map<String, Flow<ByteString>>
  ) {
    privateStorage.verifiedBatchWrite(outputLabels = outputLabelsMap, data = taskOutput)
  }

  private suspend fun markAsFinished(
    attemptKey: ExchangeStepAttemptKey,
    state: ExchangeStepAttempt.State
  ) {
    apiClient.finishExchangeStepAttempt(attemptKey, state, getAndClearTaskLog())
  }

  private suspend fun tryExecute(
    attemptKey: ExchangeStepAttemptKey,
    step: ExchangeWorkflow.Step,
    workflowIds: ExchangeWorkflow.ExchangeIdentifiers
  ) {
    val exchangeTaskDetails: ExchangeTaskDetails = exchangeTaskMapper.getExchangeTaskForStep(step)
    logger.addToTaskLog("Executing ${step.stepId} with attempt ${attemptKey.exchangeStepAttemptId}")

    timeout.runWithTimeout {

      // We only bother initializing storage if the task is marked as using it
      if (exchangeTaskDetails.readsInput ||
          exchangeTaskDetails.writesOutput ||
          exchangeTaskDetails.requiredStorageClient == ExchangeTaskStorageType.PRIVATE
      ) {
        privateStorage =
          storageSelector.getPrivateStorage(
            attemptKey,
            certificateMapper.getExchangeCertificate(
              false,
              attemptKey.recurringExchangeId,
              attemptKey.exchangeId,
              workflowIds.dataProvider,
              workflowIds.modelProvider,
              step.party
            ),
            privateKey
          )
      }

      val taskInput: Map<String, VerifiedBlob> =
        if (exchangeTaskDetails.readsInput) {
          readInputs(step.inputLabelsMap)
        } else {
          emptyMap()
        }

      val taskOutput: Map<String, Flow<ByteString>> =
        when (exchangeTaskDetails.requiredStorageClient) {
          ExchangeTaskStorageType.SHARED, ->
            exchangeTaskDetails.exchangeTask.executeWithStorage(
              taskInput,
              storageSelector.getSharedStorage(
                workflowIds.storage,
                attemptKey,
                certificateMapper.getExchangeCertificate(
                  true,
                  attemptKey.recurringExchangeId,
                  attemptKey.exchangeId,
                  workflowIds.dataProvider,
                  workflowIds.modelProvider,
                  step.party
                ),
                certificateMapper.getExchangeCertificate(
                  false,
                  attemptKey.recurringExchangeId,
                  attemptKey.exchangeId,
                  workflowIds.dataProvider,
                  workflowIds.modelProvider,
                  step.party
                ),
                privateKey
              ),
              step
            )
          ExchangeTaskStorageType.PRIVATE ->
            exchangeTaskDetails.exchangeTask.executeWithStorage(taskInput, privateStorage, step)
          else -> exchangeTaskDetails.exchangeTask.execute(taskInput)
        }
      if (exchangeTaskDetails.writesOutput) {
        writeOutputs(step.outputLabelsMap, taskOutput)
      }
    }
    markAsFinished(attemptKey, ExchangeStepAttempt.State.SUCCEEDED)
  }

  companion object {
    private val logger by loggerFor()
  }
}
