package org.wfanet.panelmatch.common.certificates.gcloud

import com.google.cloud.security.privateca.v1.Certificate
import com.google.cloud.security.privateca.v1.CreateCertificateRequest

interface CreateCertificateClient {
  suspend fun createCertificate(request: CreateCertificateRequest): Certificate
}
