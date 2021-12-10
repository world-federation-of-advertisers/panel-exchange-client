package org.wfanet.panelmatch.common.certificates.gcloud

import com.google.cloud.security.privateca.v1.Certificate
import com.google.cloud.security.privateca.v1.CertificateAuthorityServiceClient
import com.google.cloud.security.privateca.v1.CreateCertificateRequest
import org.wfanet.panelmatch.common.certificates.CreateCertificateClient

class PrivateCaClient : CreateCertificateClient, AutoCloseable {

  private val client = CertificateAuthorityServiceClient.create()

  override suspend fun createCertificate(request: CreateCertificateRequest): Certificate {
    return client.createCertificate(request)
  }

  override fun close() {
    client.close()
  }
}
