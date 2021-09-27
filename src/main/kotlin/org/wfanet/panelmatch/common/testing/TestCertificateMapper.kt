package org.wfanet.panelmatch.common.testing

import java.security.cert.X509Certificate
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.measurement.common.crypto.readCertificate
import org.wfanet.measurement.common.crypto.testing.FIXED_SERVER_CERT_PEM_FILE
import org.wfanet.panelmatch.common.CertificateMapper

class TestCertificateMapper : CertificateMapper {
  override suspend fun getExchangeCertificate(
    getOtherCert: Boolean,
    recurringExchangeId: String,
    exchangeId: String,
    dataProvider: String,
    modelProvider: String,
    executingParty: ExchangeWorkflow.Party
  ): X509Certificate {
    return readCertificate(FIXED_SERVER_CERT_PEM_FILE)
  }
}
