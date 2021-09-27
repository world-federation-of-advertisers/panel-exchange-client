package org.wfanet.panelmatch.common

import java.security.cert.X509Certificate
import org.wfanet.measurement.api.v2alpha.CertificatesGrpcKt
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.measurement.api.v2alpha.getCertificateRequest
import org.wfanet.measurement.common.crypto.readCertificate

class GrpcCertificateMapper(certificateService: CertificatesGrpcKt.CertificatesCoroutineStub) :
  CertificateMapper {

  private val certificateService = certificateService

  override suspend fun getExchangeCertificate(
    getOtherCert: Boolean,
    recurringExchangeId: String,
    exchangeId: String,
    dataProvider: String,
    modelProvider: String,
    executingParty: ExchangeWorkflow.Party
  ): X509Certificate {
    val partyName =
      when (executingParty) {
        ExchangeWorkflow.Party.DATA_PROVIDER ->
          if (getOtherCert) {
            modelProvider
          } else {
            dataProvider
          }
        ExchangeWorkflow.Party.MODEL_PROVIDER ->
          if (getOtherCert) {
            dataProvider
          } else {
            modelProvider
          }
        else -> throw IllegalArgumentException("Illegal step executor chosen")
      }
    val partyCertificate =
      certificateService.getCertificate(
        getCertificateRequest { name = "${partyName}_${recurringExchangeId}_$exchangeId" }
      )

    return readCertificate(partyCertificate.x509Der)
  }
}
