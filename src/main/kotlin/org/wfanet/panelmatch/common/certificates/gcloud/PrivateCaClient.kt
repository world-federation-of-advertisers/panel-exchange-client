package org.wfanet.panelmatch.common.certificates.gcloud

import com.google.cloud.security.privateca.v1.Certificate
import com.google.cloud.security.privateca.v1.CertificateAuthorityServiceClient
import com.google.cloud.security.privateca.v1.CreateCertificateRequest

interface CreateCertificateClient {
  suspend fun createCertificate(request: CreateCertificateRequest): Certificate
}

class PrivateCaClient : CreateCertificateClient, AutoCloseable {
  // Initialize client that will be used to send requests. This client only needs to be created
  // once, and can be reused for multiple requests. After completing all of your requests, call
  // the `certificateAuthorityServiceClient.close()` method on the client to safely
  // clean up any remaining background resources.
  private val client = CertificateAuthorityServiceClient.create()

  override suspend fun createCertificate(request: CreateCertificateRequest): Certificate {
    return client.createCertificate(request)
  }

  override fun close() {
    client.close()
  }
}
