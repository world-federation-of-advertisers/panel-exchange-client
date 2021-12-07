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
import org.wfanet.panelmatch.client.common.ExchangeContext
import org.wfanet.panelmatch.client.privatemembership.PrivateMembershipCryptor
import org.wfanet.panelmatch.common.certificates.CertificateManager
import org.wfanet.panelmatch.common.crypto.DeterministicCommutativeCipher

class GenerateKeysTasksImpl(
  private val deterministicCommutativeCryptor: DeterministicCommutativeCipher,
  private val getPrivateMembershipCryptor: (ByteString) -> PrivateMembershipCryptor,
  private val certificateManager: CertificateManager
) : GenerateKeysTasks {
  override fun getGenerateLookupKeysTask(): ExchangeTask {
    return GenerateLookupKeysTask()
  }

  override fun getGenerateSymmetricKeyTask(): ExchangeTask {
    return GenerateSymmetricKeyTask(generateKey = deterministicCommutativeCryptor::generateKey)
  }

  override fun ExchangeContext.getGenerateSerializedRlweKeysStepTask(): ExchangeTask {
    check(step.stepCase == ExchangeWorkflow.Step.StepCase.GENERATE_SERIALIZED_RLWE_KEYS_STEP)
    val privateMembershipCryptor =
      getPrivateMembershipCryptor(step.generateSerializedRlweKeysStep.serializedParameters)
    return GenerateAsymmetricKeysTask(generateKeys = privateMembershipCryptor::generateKeys)
  }

  override fun ExchangeContext.getGenerateExchangeCertificateTask(): ExchangeTask {
    return GenerateExchangeCertificateTask(certificateManager, exchangeDateKey)
  }
}
