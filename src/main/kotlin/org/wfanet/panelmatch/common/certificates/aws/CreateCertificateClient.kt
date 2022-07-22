package org.wfanet.panelmatch.common.certificates.aws

import software.amazon.awssdk.services.acmpca.model.GetCertificateRequest
import software.amazon.awssdk.services.acmpca.model.GetCertificateResponse
import software.amazon.awssdk.services.acmpca.model.IssueCertificateRequest
import software.amazon.awssdk.services.acmpca.model.IssueCertificateResponse

interface CreateCertificateClient {
  suspend fun issueCertificate(request: IssueCertificateRequest): IssueCertificateResponse
  suspend fun getCertificate(request: GetCertificateRequest): GetCertificateResponse
}
