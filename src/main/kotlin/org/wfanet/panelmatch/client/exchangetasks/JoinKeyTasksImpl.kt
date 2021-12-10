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
import org.wfanet.measurement.common.toLocalDate
import org.wfanet.panelmatch.client.common.ExchangeContext
import org.wfanet.panelmatch.client.privatemembership.PrivateMembershipCryptor
import org.wfanet.panelmatch.common.certificates.CertificateManager
import org.wfanet.panelmatch.common.crypto.DeterministicCommutativeCipher

class JoinKeyTasksImpl(
  private val deterministicCommutativeCryptor: DeterministicCommutativeCipher,
  private val getPrivateMembershipCryptor: (ByteString) -> PrivateMembershipCryptor,
  private val certificateManager: CertificateManager
) : JoinKeyTasks {
  override fun generateLookupKeys(): ExchangeTask {
    return GenerateLookupKeysTask()
  }

  override fun generateSymmetricKey(): ExchangeTask {
    return GenerateSymmetricKeyTask(generateKey = deterministicCommutativeCryptor::generateKey)
  }

  override fun generateSerializedRlweKeys(context: ExchangeContext): ExchangeTask {
    val step = context.step
    check(step.stepCase == ExchangeWorkflow.Step.StepCase.GENERATE_SERIALIZED_RLWE_KEYS_STEP)
    val privateMembershipCryptor =
      getPrivateMembershipCryptor(step.generateSerializedRlweKeysStep.serializedParameters)
    return GenerateAsymmetricKeysTask(generateKeys = privateMembershipCryptor::generateKeys)
  }

  override fun generateExchangeCertificate(context: ExchangeContext): ExchangeTask {
    return GenerateExchangeCertificateTask(certificateManager, context.exchangeDateKey)
  }

  override fun intersectAndValidate(context: ExchangeContext): ExchangeTask {
    val step = context.step
    check(step.stepCase == ExchangeWorkflow.Step.StepCase.INTERSECT_AND_VALIDATE_STEP)

    val maxSize = step.intersectAndValidateStep.maxSize
    val maximumNewItemsAllowed = step.intersectAndValidateStep.maximumNewItemsAllowed

    return IntersectValidateTask(
      maxSize = maxSize,
      maximumNewItemsAllowed = maximumNewItemsAllowed,
      isFirstExchange = context.date == context.workflow.firstExchangeDate.toLocalDate()
    )
  }
}
