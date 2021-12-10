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
  override fun generateSymmetricKey(context: ExchangeContext): ExchangeTask {
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
}
