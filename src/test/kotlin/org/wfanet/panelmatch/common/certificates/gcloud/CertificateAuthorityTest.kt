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

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.Duration
import com.google.protobuf.kotlin.toByteStringUtf8
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.measurement.common.crypto.authorityKeyIdentifier
import org.wfanet.measurement.common.crypto.readCertificate
import org.wfanet.measurement.common.crypto.sign
import org.wfanet.measurement.common.crypto.subjectKeyIdentifier
import org.wfanet.measurement.common.crypto.testing.FIXED_CA_CERT_PEM_FILE
import org.wfanet.measurement.common.crypto.verifySignature
import org.wfanet.panelmatch.common.certificates.CertificateAuthority

private val CONTEXT =
  CertificateAuthority.Context(
    organization = "mahi-ca-test-1",
    commonName = "mahi-ca-test-1-common-name",
    hostname = "google.com",
    validDays = 5
  )

private val ROOT_X509 by lazy { readCertificate(FIXED_CA_CERT_PEM_FILE) }
private val ROOT_PUBLIC_KEY by lazy { ROOT_X509.publicKey }
//private val ROOT_PRIVATE_KEY_FILE by lazy { FIXED_CA_CERT_PEM_FILE.resolveSibling("ca.key") }

val xyz = ROOT_PUBLIC_KEY

@RunWith(JUnit4::class)
class CertificateAuthorityTest {

//  projectId: String,
//  caLocation: String,
//  poolId: String,
//  private val certificateAuthorityName: String, // Why is this unused?
//  private val certificateName: String,
//  private val commonName: String,
//  private val orgName: String,
//  private val domainName: String

  @Test
  fun generatesCertificateAndCleansUpAfterwards() {
    //***  ARE THESE THE CORRECT PARAMS? ***

    val certificateAuthority =
      CertificateAuthority(
        "astute-smile-334323",
        "us-east4",
        "mahi-ca-test-1-common-name",
        "20211205-asq-atj",
        "220211205-ny6-r5m" ,
        "wfa-mahi-test-ca-name",
        "wfa-mahi-test",
        "TODO - UNSURE",
        Duration.newBuilder().setSeconds(CONTEXT.validDays.toLong() * 86400).build()
      )

    val (x509, privateKey) =
      runBlocking { certificateAuthority.generateX509CertificateAndPrivateKey() }

    assertThat(x509.notBefore.toLocalDate()).isEqualTo(LocalDate.now())
    assertThat(x509.notAfter.toLocalDate())
      .isEqualTo(LocalDate.now().plusDays(CONTEXT.validDays.toLong()))

    x509.verify(ROOT_PUBLIC_KEY) // Does not throw

    assertThat(x509.authorityKeyIdentifier).isEqualTo(ROOT_X509.subjectKeyIdentifier)

    val data = "some-data-to-be-signed".toByteStringUtf8()
    val signature = privateKey.sign(x509, data)
    assertThat(x509.verifySignature(data, signature)).isTrue()

  }
}

private fun Date.toLocalDate(): LocalDate {
  return LocalDateTime.ofInstant(toInstant(), ZoneId.systemDefault()).toLocalDate()
}
