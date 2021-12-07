package org.wfanet.panelmatch.client.exchangetasks

import com.google.protobuf.ByteString
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.panelmatch.client.common.ExchangeContext
import org.wfanet.panelmatch.client.privatemembership.PrivateMembershipCryptor
import org.wfanet.panelmatch.common.certificates.CertificateManager
import org.wfanet.panelmatch.common.crypto.DeterministicCommutativeCipher

class GenerateKeysTasksImpl : GenerateKeysTasks {
  override fun getGenerateLookupKeysTask(): ExchangeTask {
    return GenerateLookupKeysTask()
  }

  override fun getGenerateSymmetricKeyTask(
    deterministicCommutativeCryptor: DeterministicCommutativeCipher
  ): ExchangeTask {
    return GenerateSymmetricKeyTask(generateKey = deterministicCommutativeCryptor::generateKey)
  }

  override fun ExchangeContext.getGenerateSerializedRlweKeysStepTask(
    getPrivateMembershipCryptor: (ByteString) -> PrivateMembershipCryptor
  ): ExchangeTask {
    check(step.stepCase == ExchangeWorkflow.Step.StepCase.GENERATE_SERIALIZED_RLWE_KEYS_STEP)
    val privateMembershipCryptor =
      getPrivateMembershipCryptor(step.generateSerializedRlweKeysStep.serializedParameters)
    return GenerateAsymmetricKeysTask(generateKeys = privateMembershipCryptor::generateKeys)
  }

  override fun ExchangeContext.getGenerateExchangeCertificateTask(
    certificateManager: CertificateManager
  ): ExchangeTask {
    return GenerateExchangeCertificateTask(certificateManager, exchangeDateKey)
  }
}
