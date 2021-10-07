package org.wfanet.panelmatch.common.testing

import java.security.cert.X509Certificate
import org.wfanet.measurement.common.crypto.readCertificate
import org.wfanet.measurement.common.crypto.testing.FIXED_SERVER_CERT_PEM_FILE
import org.wfanet.panelmatch.common.CertificateManager

class TestCertificateManager : CertificateManager {
  override suspend fun getOwnedExchangeCertificate(
    exchangeKeyName: String,
  ): X509Certificate {
    return readCertificate(FIXED_SERVER_CERT_PEM_FILE)
  }

  override suspend fun getPartnerExchangeCertificate(
    exchangeKeyName: String,
    partnerName: String,
  ): X509Certificate {
    return readCertificate(FIXED_SERVER_CERT_PEM_FILE)
  }

  override suspend fun getPartnerExchangeRootCertificate(
    exchangeKeyName: String,
    partnerName: String
  ): X509Certificate {
    return readCertificate(FIXED_SERVER_CERT_PEM_FILE)
  }
}
