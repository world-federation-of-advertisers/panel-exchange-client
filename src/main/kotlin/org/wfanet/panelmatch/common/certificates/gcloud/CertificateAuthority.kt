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
import com.google.cloud.security.privateca.v1.X509Parameters
import com.google.cloud.security.privateca.v1.CertificateConfig
import com.google.cloud.security.privateca.v1.CertificateConfig.SubjectConfig
import com.google.cloud.security.privateca.v1.CreateCertificateRequest
import com.google.cloud.security.privateca.v1.CaPoolName
import com.google.cloud.security.privateca.v1.SubjectAltNames
import com.google.cloud.security.privateca.v1.PublicKey.KeyFormat
import com.google.protobuf.Duration
import com.google.protobuf.kotlin.toByteString
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.X509Certificate
import org.wfanet.measurement.common.crypto.generateKeyPair
import org.wfanet.measurement.common.crypto.readCertificate
import org.wfanet.panelmatch.common.certificates.CertificateAuthority
import org.wfanet.panelmatch.common.loggerFor

class CertificateAuthority(
  private val projectId: String,
  private val caLocation: String,
  private val poolId: String,
  private val certificateAuthorityName: String,
  private val certificateName: String,
  private val commonName: String,
  private val orgName: String,
  private val domainName: String,
  private val certificateLifetime: Duration,
  private val client: CreateCertificateClient,
  private val x509Parameters: X509Parameters,
  private val cloudPublicKey: PublicKey

) : CertificateAuthority {

  override suspend fun generateX509CertificateAndPrivateKey(): Pair<X509Certificate, PrivateKey> {

    val keyPair: KeyPair = generateKeyPair("EC")
    val privateKey: PrivateKey = keyPair.private
    var publicKey: PublicKey = keyPair.public

    if (cloudPublicKey != null) {
      publicKey = cloudPublicKey
    }

    // Set the Public Key and its format.
    val cloudPublicKeyInput = CloudPublicKey.newBuilder().setKey(publicKey.encoded.toByteString()).setFormat(KeyFormat.PEM).build()

    val subjectConfig =
      SubjectConfig.newBuilder() // Set the common name and org name.
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
            .setPublicKey(cloudPublicKeyInput)
            .setSubjectConfig(subjectConfig)
            .setX509Config(x509Parameters)
            .build()
        )
        .setLifetime(
          certificateLifetime
        )
        .build()

    // Create the Certificate Request.
    val certificateRequest = CreateCertificateRequest.newBuilder()
      .setParent(CaPoolName.of(projectId, caLocation, poolId).toString())
      .setCertificateId(certificateName)
      .setCertificate(certificate)
      .setIssuingCertificateAuthorityId(certificateAuthorityName)
      .build()

      // Get the Certificate response.
    val response = client.createCertificate(certificateRequest)

    return readCertificate(response.pemCertificate.byteInputStream()) to privateKey
  }

  companion object {
    private val logger by loggerFor()
  }
}
