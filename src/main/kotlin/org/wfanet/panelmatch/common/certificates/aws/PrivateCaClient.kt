package org.wfanet.panelmatch.common.certificates.aws

import software.amazon.awssdk.services.acmpca.AcmPcaClient
import software.amazon.awssdk.services.acmpca.model.GetCertificateRequest
import software.amazon.awssdk.services.acmpca.model.GetCertificateResponse
import software.amazon.awssdk.services.acmpca.model.IssueCertificateRequest
import software.amazon.awssdk.services.acmpca.model.IssueCertificateResponse

class PrivateCaClient : CreateCertificateClient, AutoCloseable {
  private val client = AcmPcaClient.create()

  override suspend fun issueCertificate(request: IssueCertificateRequest): IssueCertificateResponse {
    return client.issueCertificate(request)
  }

  override suspend fun getCertificate(request: GetCertificateRequest): GetCertificateResponse {
    return client.getCertificate(request)
  }

  override fun close() {
    client.close()
  }
}
