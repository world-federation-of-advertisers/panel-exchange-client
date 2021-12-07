package org.wfanet.panelmatch.client.exchangetasks

import com.google.protobuf.ByteString
import org.wfanet.panelmatch.client.common.ExchangeContext
import org.wfanet.panelmatch.client.privatemembership.PrivateMembershipCryptor
import org.wfanet.panelmatch.common.certificates.CertificateManager
import org.wfanet.panelmatch.common.crypto.DeterministicCommutativeCipher

interface GenerateKeysTasks {

  fun getGenerateSymmetricKeyTask(
    deterministicCommutativeCryptor: DeterministicCommutativeCipher
  ): ExchangeTask

  fun getGenerateLookupKeysTask(): ExchangeTask

  fun ExchangeContext.getGenerateSerializedRlweKeysStepTask(
    getPrivateMembershipCryptor: (ByteString) -> PrivateMembershipCryptor
  ): ExchangeTask

  fun ExchangeContext.getGenerateExchangeCertificateTask(
    certificateManager: CertificateManager
  ): ExchangeTask
}
