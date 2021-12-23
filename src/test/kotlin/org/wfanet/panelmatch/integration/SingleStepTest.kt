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

package org.wfanet.panelmatch.integration

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.toByteStringUtf8
import java.lang.IllegalArgumentException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern
import org.junit.runners.Parameterized
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.measurement.api.v2alpha.exchangeWorkflow
import org.wfanet.measurement.common.getRuntimePath
import org.wfanet.measurement.common.readByteString
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow.Step.StepCase

private val FIXTURES_FILES_PATH: Path =
  checkNotNull(
    getRuntimePath(
      Paths.get(
        "panel_exchange_client",
        "src",
        "main",
        "kotlin",
        "org",
        "wfanet",
        "panelmatch",
        "integration",
        "fixtures"
      )
    )
  )

@RunWith(Parameterized::class)
class SingleStepTest(filteredStep: ExchangeWorkflow.Step) : AbstractInProcessPanelMatchIntegrationTest() {

  private val party:ExchangeWorkflow.Party = filteredStep.party
  private val neededInputs: Map<String, String> = filteredStep.inputLabelsMap
  private val neededOutputs: Map<String, String> = filteredStep.outputLabelsMap
  override val initialDataProviderInputs by lazy {
    if (filteredStep.stepCase === StepCase.COPY_FROM_SHARED_STORAGE_STEP)
      mutableMapOf()
    else if (filteredStep.stepCase === StepCase.INPUT_STEP)
      getData(neededOutputs, party)
    else
      getData(neededInputs, party)
  }
  override val initialModelProviderInputs by lazy {
    if (filteredStep.stepCase === StepCase.COPY_FROM_SHARED_STORAGE_STEP)
      mutableMapOf()
    else if (filteredStep.stepCase === StepCase.INPUT_STEP)
      getData(neededOutputs, party)
    else
      getData(neededInputs, party)
  }
  override val initialSharedInputs by lazy {
    if (filteredStep.stepCase === StepCase.COPY_FROM_SHARED_STORAGE_STEP)
      getData(neededInputs, null)
    else
      mutableMapOf()
  }

  override val providedExchangeWorkflow by lazy { exchangeWorkflow { steps += filteredStep } }

  private val dataProviderOutputs by lazy {
    if (filteredStep.stepCase === StepCase.COPY_TO_SHARED_STORAGE_STEP || party !== ExchangeWorkflow.Party.DATA_PROVIDER)
      emptyMap()
    else
      getData(neededOutputs, party)
  }

  private val modelProviderOutputs by lazy {
    if (filteredStep.stepCase === StepCase.COPY_TO_SHARED_STORAGE_STEP || party !== ExchangeWorkflow.Party.MODEL_PROVIDER)
      emptyMap()
    else
      getData(neededOutputs, party)
  }

  override val finalSharedOutputs by lazy {
    if (filteredStep.stepCase === StepCase.COPY_TO_SHARED_STORAGE_STEP)
      getData(neededOutputs, null)
    else
      emptyMap()
  }

  override fun validateFinalState(
    dataProviderDaemon: ExchangeWorkflowDaemonForTest,
    modelProviderDaemon: ExchangeWorkflowDaemonForTest
  ) {
    for ((key, value) in dataProviderOutputs) {
      assertThat(dataProviderDaemon.readPrivateBlob(key)).isEqualTo(value)
    }
    for ((key, value) in modelProviderOutputs) {
      assertThat(modelProviderDaemon.readPrivateBlob(key)).isEqualTo(value)
    }
  }

  companion object {
    // TODO: Update to preprocessing workflow
    private const val fullExchangeWorkflowResourcePath: String = "config/full_exchange_workflow.textproto"

    @JvmStatic
    @Parameterized.Parameters(name = "{index}: Test with Step={0}")
    fun stepsToTest():List<ExchangeWorkflow.Step> {
      return getExchangeWorkflow((fullExchangeWorkflowResourcePath)).stepsList
    }

    private val manifestNumberPattern = Pattern.compile("\\d+")
    private fun getData(
      neededData: Map<String, String>,
      currentParty: ExchangeWorkflow.Party?
    ): MutableMap<String, ByteString> {
      val inputs = mutableMapOf<String, ByteString>()
      val folderName =
        when (currentParty) {
          ExchangeWorkflow.Party.DATA_PROVIDER -> "edp"
          ExchangeWorkflow.Party.MODEL_PROVIDER -> "mp"
          else -> "shared"
        }
      for ((_, value) in neededData) {
        val data = FIXTURES_FILES_PATH.resolve(folderName).resolve(value).toFile().readByteString()
        inputs[value] = data
        if (folderName === "shared"){
          val signatureValue = "$value.signature"
          val signatureData = FIXTURES_FILES_PATH.resolve(folderName).resolve(signatureValue).toFile().readByteString()
          inputs[signatureValue] = signatureData
        }
        val dataString = data.toStringUtf8()
        if (dataString.contains("-*-of")) {
          val matcher = manifestNumberPattern.matcher(dataString)
          require(matcher.find())
          val numShards = matcher.group().toInt() - 1
          for (i in 0..numShards) {
            val key = dataString.replace("*", i.toString())
            inputs[key] =
              FIXTURES_FILES_PATH.resolve(folderName).resolve(key).toFile().readByteString()
          }
        }
      }
      return inputs
    }
  }
}
