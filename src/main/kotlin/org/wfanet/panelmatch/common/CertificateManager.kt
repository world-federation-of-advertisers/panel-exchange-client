package org.wfanet.panelmatch.common

import java.security.cert.X509Certificate

interface CertificateManager {

  suspend fun getOwnedExchangeCertificate(
    recurringExchangeId: String,
    exchangeId: String
  ): X509Certificate

  suspend fun getPartnerExchangeCertificate(
    recurringExchangeId: String,
    exchangeId: String,
    partnerName: String,
  ): X509Certificate
}
