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

package org.wfanet.panelmatch.common.certificates.gcloud

import com.google.cloud.security.privateca.v1.PublicKey as CloudPublicKey
import com.google.cloud.security.privateca.v1.Certificate
import com.google.cloud.security.privateca.v1.Subject
import com.google.cloud.security.privateca.v1.CertificateConfig
import com.google.cloud.security.privateca.v1.CreateCertificateRequest
import com.google.cloud.security.privateca.v1.CaPoolName
import com.google.cloud.security.privateca.v1.SubjectAltNames
import com.google.cloud.security.privateca.v1.PublicKey.KeyFormat
import com.google.protobuf.Duration
import com.google.protobuf.kotlin.toByteString
import java.security.PublicKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.measurement.common.crypto.readCertificate
import org.wfanet.measurement.common.crypto.testing.FIXED_CA_CERT_PEM_FILE
import com.google.common.truth.Truth.assertThat
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.whenever
import org.wfanet.panelmatch.common.certificates.CertificateAuthority

private val CONTEXT =
  CertificateAuthority.Context(
    projectId = "some-project-id",
    caLocation = "some-ca-location",
    poolId = "some-pool-id",
    certificateAuthorityName = "some-certificate-authority-name",
    certificateName = "some-certificate-name",
    commonName = "some-common-name",
    orgName = "some-org-name",
    domainName ="some-domain-name",
    hostname = "example.com",
    validDays = 5,
  )

private val ROOT_X509 by lazy { readCertificate(FIXED_CA_CERT_PEM_FILE) }
private val ROOT_PUBLIC_KEY by lazy { ROOT_X509.publicKey }

@RunWith(JUnit4::class)
class CertificateAuthorityTest {

  @Test
  suspend fun mockGenerateX509CertificateAndPrivateKeyTest() {

    val mockCreateCertificateClient: CreateCertificateClient =
      mock(CreateCertificateClient::class.java)

    val createCertificateRequest: CreateCertificateRequest =
      generateCreateCertificateRequest(CONTEXT, ROOT_PUBLIC_KEY)

    whenever(mockCreateCertificateClient.createCertificate(any()))
      .thenReturn(Certificate.newBuilder().setPemCertificate(ROOT_PUBLIC_KEY.toString()).build())

    val certificateAuthority =
      CertificateAuthority(
        CONTEXT,
        mockCreateCertificateClient,
      )

    val (x509, privateKey) =
      runBlocking { certificateAuthority.generateX509CertificateAndPrivateKey() }

    argumentCaptor<CreateCertificateRequest> {
      verify(mockCreateCertificateClient).createCertificate(capture())
      assertThat(firstValue).isEqualTo(
        createCertificateRequest
      )
    }

    assertThat(x509.publicKey).isEqualTo(ROOT_PUBLIC_KEY)
  }

}

fun generateCreateCertificateRequest(
  context: CertificateAuthority.Context,
  root_public_key: PublicKey
) : CreateCertificateRequest{

  val certificateLifetime: Duration =
    Duration.newBuilder().setSeconds(context.validDays.toLong() * 86400).build()

  // Set the Public Key and its format.
  val cloudPublicKey: CloudPublicKey = CloudPublicKey.newBuilder().setKey(root_public_key.encoded.toByteString()).setFormat(KeyFormat.PEM).build()

  val subjectConfig =
    CertificateConfig.SubjectConfig.newBuilder() // Set the common name and org name.
      .setSubject(
        Subject.newBuilder().setCommonName(context.commonName)
          .setOrganization(context.orgName).build()
      ) // Set the fully qualified domain name.
      .setSubjectAltName(SubjectAltNames.newBuilder().addDnsNames(context.domainName).build())
      .build()

  // Create certificate.
  val certificate: Certificate =
    Certificate.newBuilder()
      .setConfig(
        CertificateConfig.newBuilder()
          .setPublicKey(cloudPublicKey)
          .setSubjectConfig(subjectConfig)
          .setX509Config(X509PARAMETERS)
          .build()
      )
      .setLifetime(
        certificateLifetime
      )
      .build()

  // Create the Certificate Request.
  return CreateCertificateRequest.newBuilder()
    .setParent(CaPoolName.of(context.projectId, context.caLocation, context.poolId).toString())
    .setCertificateId(context.certificateName)
    .setCertificate(certificate)
    .setIssuingCertificateAuthorityId(context.certificateAuthorityName)
    .build()
}

private fun Date.toLocalDate(): LocalDate {
  return LocalDateTime.ofInstant(toInstant(), ZoneId.systemDefault()).toLocalDate()
}
