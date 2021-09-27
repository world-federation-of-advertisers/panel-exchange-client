package org.wfanet.panelmatch.common

import java.security.cert.X509Certificate
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow

interface CertificateMapper {
  suspend fun getExchangeCertificate(
    getOtherCert: Boolean,
    recurringExchangeId: String,
    exchangeId: String,
    dataProvider: String,
    modelProvider: String,
    executingParty: ExchangeWorkflow.Party
  ): X509Certificate
}
