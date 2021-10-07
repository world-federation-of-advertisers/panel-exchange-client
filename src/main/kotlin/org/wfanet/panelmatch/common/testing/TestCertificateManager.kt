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

package org.wfanet.panelmatch.common.testing

import java.security.cert.X509Certificate
import org.wfanet.measurement.api.v2alpha.ExchangeKey
import org.wfanet.measurement.common.crypto.readCertificate
import org.wfanet.measurement.common.crypto.testing.FIXED_SERVER_CERT_PEM_FILE
import org.wfanet.panelmatch.common.CertificateManager

class TestCertificateManager : CertificateManager {
  override suspend fun getOwnedExchangeCertificate(
    exchangeKey: ExchangeKey,
  ): X509Certificate {
    return readCertificate(FIXED_SERVER_CERT_PEM_FILE)
  }

  override suspend fun getPartnerExchangeCertificate(
    exchangeKey: ExchangeKey,
    partnerName: String,
  ): X509Certificate {
    return readCertificate(FIXED_SERVER_CERT_PEM_FILE)
  }

  override suspend fun getPartnerExchangeRootCertificate(partnerName: String): X509Certificate {
    return readCertificate(FIXED_SERVER_CERT_PEM_FILE)
  }
}
