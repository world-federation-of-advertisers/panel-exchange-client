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

package org.wfanet.panelmatch.common

import java.security.cert.X509Certificate
import org.wfanet.measurement.api.v2alpha.ExchangeKey

/**
 * Interface to grab and validate certificates. Provides validated X509 certificates for an exchange
 * to use to validate .
 */
interface CertificateManager {

  /** Grabs certificates generated by the executing party. */
  suspend fun getOwnedExchangeCertificate(exchangeKey: ExchangeKey): X509Certificate

  /** Grabs certificates generated by the other party in the exchange. */
  suspend fun getPartnerExchangeCertificate(
    exchangeKey: ExchangeKey,
    partnerName: String,
  ): X509Certificate

  /** Grabs a root certificate for a partner of the caller. */
  suspend fun getPartnerExchangeRootCertificate(
    partnerName: String,
  ): X509Certificate
}
