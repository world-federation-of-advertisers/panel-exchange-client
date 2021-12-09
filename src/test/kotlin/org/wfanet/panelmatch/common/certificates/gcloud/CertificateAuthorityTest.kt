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

import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.mock
import org.wfanet.measurement.common.crypto.readCertificate
import org.wfanet.panelmatch.common.certificates.CertificateAuthority
import com.google.cloud.security.privateca.v1.Certificate
import com.google.cloud.security.privateca.v1.Subject
import com.google.cloud.security.privateca.v1.X509Parameters
import com.google.cloud.security.privateca.v1.CertificateConfig
import com.google.cloud.security.privateca.v1.CreateCertificateRequest
import com.google.cloud.security.privateca.v1.CaPoolName
import com.google.cloud.security.privateca.v1.SubjectAltNames
import com.google.cloud.security.privateca.v1.KeyUsage;
import com.google.cloud.security.privateca.v1.KeyUsage.ExtendedKeyUsageOptions
import com.google.cloud.security.privateca.v1.KeyUsage.KeyUsageOptions
import com.google.cloud.security.privateca.v1.X509Parameters.CaOptions
import com.google.common.base.Predicates.instanceOf
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.Duration
import com.google.protobuf.kotlin.toByteString
import org.wfanet.measurement.common.crypto.testing.FIXED_CA_CERT_PEM_FILE

private val CONTEXT =
  CertificateAuthority.Context(
    organization = "some-organization",
    commonName = "some-common-name",
    hostname = "example.com",
    validDays = 5
  )

private val ROOT_X509 by lazy { readCertificate(FIXED_CA_CERT_PEM_FILE) }
private val ROOT_PUBLIC_KEY by lazy { ROOT_X509.publicKey }

// Set the X.509 fields required for the certificate.
private val X509PARAMETERS = X509Parameters.newBuilder()
  .setKeyUsage(
    KeyUsage.newBuilder()
      .setBaseKeyUsage(
        KeyUsageOptions.newBuilder()
          .setDigitalSignature(true)
          .setKeyEncipherment(true)
          .setCertSign(true)
          .build()
      )
      .setExtendedKeyUsage(
        ExtendedKeyUsageOptions.newBuilder().setServerAuth(true).build()
      )
      .build()
  )
  .setCaOptions(CaOptions.newBuilder().setIsCa(true).buildPartial())
  .build()

private val DURATION: Duration =
  Duration.newBuilder().setSeconds(CONTEXT.validDays.toLong() * 86400).build()

private val CREATECERTIFICATECLIENT: CreateCertificateClient = mock<PrivateCaClient>()

@RunWith(JUnit4::class)
class CertificateAuthorityTest {

  @Test
  fun mockGenerateX509CertificateAndPrivateKeyTest() {

    val certificateAuthority =
      CertificateAuthority(
        "some-project-id",
        "some-ca-location",
        "some-pool-id",
        "some-certificate-authority-name",
        "some-certificate-name" ,
        "some-common-name",
        "some-org-name",
        "some-domain-name",
        DURATION,
        CREATECERTIFICATECLIENT,
        X509PARAMETERS
      )

    val (x509, privateKey) =
      runBlocking { certificateAuthority.generateX509CertificateAndPrivateKey() }

    assertThat(instanceOf(privateKey::class.java)).isEqualTo(instanceOf(PrivateKey::class.java))
    assertThat(instanceOf(x509::class.java)).isEqualTo(instanceOf(X509Certificate::class.java))
  }

  @Test
  suspend fun SpecificCertificateAuthorityRequestTest() {

    val createCertificateRequest: CreateCertificateRequest =
      generateCreateCertificateRequest("astute-smile-334323",
        "us-east4",
        "mahi-ca-test-1-common-name",
        "20211205-asq-atj",
        "220211205-ny6-r5m" ,
        "wfa-mahi-test-ca-name",
        "wfa-mahi-test",
        "TODO - UNSURE", DURATION)

    val response = CREATECERTIFICATECLIENT.createCertificate(createCertificateRequest)
    val x509Certificate = readCertificate(response.pemCertificate.byteInputStream())

    assertThat(instanceOf(x509Certificate::class.java)).isEqualTo(instanceOf(X509Certificate::class.java))
  }
}

private fun generateCreateCertificateRequest(projectId: String, caLocation: String, poolId: String,
                                             certificateAuthorityName: String, certificateName: String,
                                             commonName: String, orgName: String, domainName: String,
                                             certificateLifetime: Duration,) : CreateCertificateRequest{

  // Set the Public Key and its format.
  val cloudPublicKey: com.google.cloud.security.privateca.v1.PublicKey =
    com.google.cloud.security.privateca.v1.PublicKey.newBuilder().setKey(ROOT_PUBLIC_KEY.encoded.toByteString()).setFormat(
      com.google.cloud.security.privateca.v1.PublicKey.KeyFormat.PEM).build()

  val subjectConfig =
    CertificateConfig.SubjectConfig.newBuilder() // Set the common name and org name.
      .setSubject(
        Subject.newBuilder().setCommonName(commonName)
          .setOrganization(orgName).build()
      ) // Set the fully qualified domain name.
      .setSubjectAltName(SubjectAltNames.newBuilder().addDnsNames(domainName).build())
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
    .setParent(CaPoolName.of(projectId, caLocation, poolId).toString())
    .setCertificateId(certificateName)
    .setCertificate(certificate)
    .setIssuingCertificateAuthorityId(certificateAuthorityName)
    .build()
}

private fun Date.toLocalDate(): LocalDate {
  return LocalDateTime.ofInstant(toInstant(), ZoneId.systemDefault()).toLocalDate()
}
