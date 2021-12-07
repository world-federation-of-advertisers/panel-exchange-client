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
import com.google.cloud.security.privateca.v1.KeyUsage
import com.google.cloud.security.privateca.v1.Certificate
import com.google.cloud.security.privateca.v1.Subject
import com.google.cloud.security.privateca.v1.CertificateAuthorityServiceClient
import com.google.cloud.security.privateca.v1.X509Parameters
import com.google.cloud.security.privateca.v1.CertificateConfig
import com.google.cloud.security.privateca.v1.CreateCertificateRequest
import com.google.cloud.security.privateca.v1.CaPoolName
import com.google.cloud.security.privateca.v1.SubjectAltNames
import com.google.cloud.security.privateca.v1.CertificateConfig.SubjectConfig
import com.google.cloud.security.privateca.v1.KeyUsage.ExtendedKeyUsageOptions
import com.google.cloud.security.privateca.v1.KeyUsage.KeyUsageOptions
import com.google.cloud.security.privateca.v1.X509Parameters.CaOptions
import com.google.cloud.security.privateca.v1.PublicKey.KeyFormat
import com.google.api.core.ApiFuture
import com.google.protobuf.ByteString
import com.google.protobuf.Duration
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.X509Certificate
import org.wfanet.panelmatch.common.certificates.CertificateAuthority
import org.wfanet.panelmatch.common.loggerFor
import java.security.KeyPair
import org.wfanet.measurement.common.crypto.generateKeyPair
import org.wfanet.measurement.common.crypto.readCertificate

class CertificateAuthority(
  private val projectId: String,
  private val caLocation: String,
  private val poolId: String,
  private val certificateAuthorityName: String,
  private val certificateName: String,
  private val commonName: String,
  private val orgName: String,
  private val domainName: String,
  private val certificateLifetime: Duration

) : CertificateAuthority {

  override suspend fun generateX509CertificateAndPrivateKey(): Pair<X509Certificate, PrivateKey> {
    // Initialize client that will be used to send requests. This client only needs to be created
    // once, and can be reused for multiple requests. After completing all of your requests, call
    // the `certificateAuthorityServiceClient.close()` method on the client to safely
    // clean up any remaining background resources.

    CertificateAuthorityServiceClient.create().use { certificateAuthorityServiceClient ->

      // ***  NOT SURE ON CORRECT IMPORT + INPUT TO CREATE PUBLIC/PRIVATE KEY ***
      val keyPair: KeyPair = generateKeyPair("TODO")

      val privateKey: PrivateKey = keyPair.private
      val publicKey: PublicKey = keyPair.public

      // Set the Public Key and its format.
      val cloudPublicKey: CloudPublicKey =
        CloudPublicKey.newBuilder().setKey(ByteString.copyFromUtf8(publicKey.toString())).setFormat(KeyFormat.PEM).build()

      val subjectConfig =
        SubjectConfig.newBuilder() // Set the common name and org name.
          .setSubject(
            Subject.newBuilder().setCommonName(commonName)
              .setOrganization(orgName).build()
          ) // Set the fully qualified domain name.
          .setSubjectAltName(SubjectAltNames.newBuilder().addDnsNames(domainName).build())
          .build()

      // Set the X.509 fields required for the certificate.
      val x509Parameters = X509Parameters.newBuilder()
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

      // Create certificate.
      val certificate: Certificate =
        Certificate.newBuilder()
          .setConfig(
            CertificateConfig.newBuilder()
              .setPublicKey(cloudPublicKey)
              .setSubjectConfig(subjectConfig)
              .setX509Config(x509Parameters)
              .build()
          )
          .setLifetime(
            certificateLifetime
          )
          .build()

      // Create the Certificate Request.
      val certificateRequest: CreateCertificateRequest = CreateCertificateRequest.newBuilder()
        .setParent(CaPoolName.of(projectId, caLocation, poolId).toString())
        .setCertificateId(certificateName)
        .setCertificate(certificate)
        .setIssuingCertificateAuthorityId(certificateAuthorityName)
        .build()

      // Get the Certificate response.
      val future: ApiFuture<Certificate> =
        certificateAuthorityServiceClient
          .createCertificateCallable()
          .futureCall(certificateRequest)

      val response: Certificate = future.get()

      return readCertificate(response.pemCertificate.byteInputStream()) to privateKey
    }
  }

  companion object {
    private val logger by loggerFor()
  }
}
