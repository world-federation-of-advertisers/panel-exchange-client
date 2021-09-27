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

package org.wfanet.panelmatch.client.launcher.testing

import com.google.protobuf.ByteString
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wfanet.measurement.api.v2alpha.ExchangeStep
import org.wfanet.measurement.api.v2alpha.ExchangeStep.SignedExchangeWorkflow
import org.wfanet.measurement.api.v2alpha.ExchangeStepKt.signedExchangeWorkflow
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow.Step
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflowKt.StepKt.inputStep
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflowKt.exchangeIdentifiers
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflowKt.step
import org.wfanet.measurement.api.v2alpha.exchangeStep
import org.wfanet.measurement.api.v2alpha.exchangeWorkflow
import org.wfanet.panelmatch.common.toByteString
import org.wfanet.panelmatch.protocol.common.DeterministicCommutativeCipher

val MP_0_SECRET_KEY: ByteString = "random-mp-string-0".toByteString()

val JOIN_KEYS =
  listOf(
    "some joinkey0".toByteString(),
    "some joinkey1".toByteString(),
    "some joinkey2".toByteString(),
    "some joinkey3".toByteString(),
    "some joinkey4".toByteString()
  )

val SINGLE_BLINDED_KEYS =
  listOf(
    "some single-blinded key0".toByteString(),
    "some single-blinded key1".toByteString(),
    "some single-blinded key2".toByteString(),
    "some single-blinded key3".toByteString(),
    "some single-blinded key4".toByteString()
  )
val DOUBLE_BLINDED_KEYS =
  listOf(
    "some double-blinded key0".toByteString(),
    "some double-blinded key1".toByteString(),
    "some double-blinded key2".toByteString(),
    "some double-blinded key3".toByteString(),
    "some double-blinded key4".toByteString()
  )
val LOOKUP_KEYS =
  listOf(
    "some lookup0".toByteString(),
    "some lookup1".toByteString(),
    "some lookup2".toByteString(),
    "some lookup3".toByteString(),
    "some lookup4".toByteString()
  )

fun buildMockCryptor(): DeterministicCommutativeCipher {
  val mockCryptor: DeterministicCommutativeCipher = mock()
  whenever(mockCryptor.encrypt(any(), any())).thenReturn(SINGLE_BLINDED_KEYS)
  whenever(mockCryptor.reEncrypt(any(), any())).thenReturn(DOUBLE_BLINDED_KEYS)
  whenever(mockCryptor.decrypt(any(), any())).thenReturn(LOOKUP_KEYS)
  return mockCryptor
}

fun buildWorkflow(
  testedStep: Step,
  dataProviderName: String,
  modelProviderName: String
): ExchangeWorkflow {
  return exchangeWorkflow {
    steps += testedStep

    exchangeIdentifiers =
      exchangeIdentifiers {
        dataProvider = dataProviderName
        modelProvider = modelProviderName
      }
  }
}

fun buildSignedExchangeWorkflow(exchangeWorkflow: ExchangeWorkflow): SignedExchangeWorkflow {
  return signedExchangeWorkflow {
    this.serializedExchangeWorkflow = exchangeWorkflow.toByteString()
  }
}

fun buildExchangeStep(
  name: String,
  stepIndex: Int = 0,
  dataProviderName: String,
  modelProviderName: String,
  testedStep: Step
): ExchangeStep {
  return exchangeStep {
    this.stepIndex = stepIndex
    this.name = name
    this.signedExchangeWorkflow =
      buildSignedExchangeWorkflow(buildWorkflow(testedStep, dataProviderName, modelProviderName))
  }
}

fun inputStep(label: Pair<String, String>): Step {
  return step {
    inputStep = inputStep {}
    outputLabels[label.first] = label.second
  }
}
