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

package org.wfanet.panelmatch.common.certificates.openssl

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.kotlin.toByteStringUtf8
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.measurement.common.crypto.authorityKeyIdentifier
import org.wfanet.measurement.common.crypto.readCertificate
import org.wfanet.measurement.common.crypto.sign
import org.wfanet.measurement.common.crypto.subjectKeyIdentifier
import org.wfanet.measurement.common.crypto.testing.FIXED_SERVER_CERT_PEM_FILE
import org.wfanet.measurement.common.crypto.testing.FIXED_SERVER_KEY_DER_FILE
import org.wfanet.measurement.common.crypto.verifySignature
import org.wfanet.panelmatch.common.certificates.CertificateAuthority

private val CONTEXT =
  CertificateAuthority.Context(
    organization = "some-organization",
    commonName = "some-common-name",
    hostname = "example.com",
    validDays = 5
  )

private val ROOT_X509 by lazy { readCertificate(FIXED_SERVER_CERT_PEM_FILE) }
private val ROOT_PUBLIC_KEY by lazy { ROOT_X509.publicKey }

@RunWith(JUnit4::class)
class OpenSslCertificateAuthorityTest {
  @get:Rule val temporaryFolder = TemporaryFolder()

  @Test
  fun generatesCertificateAndCleansUpAfterwards() {
    val certificateAuthority =
      OpenSslCertificateAuthority(CONTEXT, FIXED_SERVER_KEY_DER_FILE, temporaryFolder.root)

    val (x509, privateKey) =
      runBlocking { certificateAuthority.generateX509CertificateAndPrivateKey(ROOT_PUBLIC_KEY) }

    assertThat(x509.notBefore.toLocalDate()).isEqualTo(LocalDate.now())
    assertThat(x509.notAfter.toLocalDate())
      .isEqualTo(LocalDate.now().plusDays(CONTEXT.validDays.toLong()))

    x509.verify(ROOT_PUBLIC_KEY) // Does not throw

    assertThat(x509.authorityKeyIdentifier).isEqualTo(ROOT_X509.subjectKeyIdentifier)

    val data = "some-data-to-be-signed".toByteStringUtf8()
    val signature = privateKey.sign(x509, data)
    assertThat(x509.verifySignature(data, signature)).isTrue()

    // Ensure OpenSSL input and output files are cleaned up:
    assertThat(temporaryFolder.root.list()).isEmpty()
  }
}

private fun Date.toLocalDate(): LocalDate {
  return LocalDateTime.ofInstant(toInstant(), ZoneId.systemDefault()).toLocalDate()
}
