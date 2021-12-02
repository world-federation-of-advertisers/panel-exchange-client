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

import com.google.cloud.security.privateca.v1.CaPoolName
import com.google.cloud.security.privateca.v1.Certificate
import com.google.cloud.security.privateca.v1.CertificateAuthorityServiceClient
import com.google.cloud.security.privateca.v1.CertificateConfig
import com.google.cloud.security.privateca.v1.KeyUsage
import com.google.cloud.security.privateca.v1.PublicKey as CloudPublicKey
import com.google.cloud.security.privateca.v1.PublicKey.KeyFormat
import com.google.cloud.security.privateca.v1.Subject
import com.google.cloud.security.privateca.v1.SubjectAltNames
import com.google.cloud.security.privateca.v1.X509Parameters
import com.google.protobuf.Duration
import com.google.protobuf.kotlin.toByteString
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.X509Certificate
import org.wfanet.panelmatch.common.certificates.CertificateAuthority
import org.wfanet.panelmatch.common.loggerFor

class CertificateAuthority(
  projectId: String,
  caLocation: String,
  poolId: String,
  private val certificateAuthorityName: String, // Why is this unused?
  private val certificateName: String,
  private val commonName: String,
  private val orgName: String,
  private val domainName: String
) : CertificateAuthority {

  private val certificateAuthorityServiceClient = CertificateAuthorityServiceClient.create()
  private val caPoolName = CaPoolName.of(projectId, caLocation, poolId).toString()

  override suspend fun generateX509CertificateAndPrivateKey(
    rootPublicKey: PublicKey,
  ): Pair<X509Certificate, PrivateKey> {
    val certificateLifetime = 1000L

    // Set the Public Key and its format.
    val publicKey: CloudPublicKey =
      CloudPublicKey.newBuilder()
        .setKey(rootPublicKey.encoded.toByteString())
        .setFormat(KeyFormat.PEM)
        .build()

    val subjectConfig: CertificateConfig.SubjectConfig =
      CertificateConfig.SubjectConfig.newBuilder() // Set the common name and org name.
        .setSubject(
          Subject.newBuilder().setCommonName(commonName).setOrganization(orgName).build()
        ) // Set the fully qualified domain name.
        .setSubjectAltName(SubjectAltNames.newBuilder().addDnsNames(domainName).build())
        .build()

    // Set the X.509 fields required for the certificate.
    val x509Parameters: X509Parameters =
      X509Parameters.newBuilder()
        .setKeyUsage(
          KeyUsage.newBuilder()
            .setBaseKeyUsage(
              KeyUsage.KeyUsageOptions.newBuilder()
                .setDigitalSignature(true)
                .setKeyEncipherment(true)
                .setCertSign(true)
                .build()
            )
            .setExtendedKeyUsage(
              KeyUsage.ExtendedKeyUsageOptions.newBuilder().setServerAuth(true).build()
            )
            .build()
        )
        .setCaOptions(X509Parameters.CaOptions.newBuilder().setIsCa(true).buildPartial())
        .build()

    // Create certificate.
    val certificate: Certificate =
      Certificate.newBuilder()
        .setConfig(
          CertificateConfig.newBuilder()
            .setPublicKey(publicKey)
            .setSubjectConfig(subjectConfig)
            .setX509Config(x509Parameters)
            .build()
        )
        .setLifetime(Duration.newBuilder().setSeconds(certificateLifetime).build())
        .build()

    val responseCertificate =
      certificateAuthorityServiceClient.createCertificate(caPoolName, certificate, certificateName)

    logger.info(responseCertificate.toString())

    TODO()
  }

  companion object {
    private val logger by loggerFor()
  }
}
