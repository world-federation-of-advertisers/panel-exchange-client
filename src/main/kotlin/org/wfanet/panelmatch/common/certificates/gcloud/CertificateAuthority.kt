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
import com.google.api.core.ApiFuture
import com.google.protobuf.Duration
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.X509Certificate
import org.wfanet.panelmatch.common.certificates.CertificateAuthority
import org.wfanet.panelmatch.common.loggerFor
//import org.wfanet.measurement.common.crypto.generateKeyPair

// https://github.com/googleapis/java-security-private-ca/blob/6650af45214f871041e3eb91214b50332ab6ce94/samples/snippets/cloud-client/src/main/java/privateca/CreateCertificate.java
// Would the code be the exact same as this , can I do what the CreateCertificate is doing
// Where do we get the private key from?

class CertificateAuthority(
  private val projectId: String,
  private val caLocation: String,
  private val poolId: String,
  private val certificateAuthorityName: String,
  private val certificateName: String,
  private val commonName: String,
  private val orgName: String,
  private val domainName: String,
  private val publicKey: PublicKey,
  private val certificateLifetime: Long

) : CertificateAuthority {

  override suspend fun generateX509CertificateAndPrivateKey(): Pair<X509Certificate, PrivateKey> {
    // Initialize client that will be used to send requests. This client only needs to be created
    // once, and can be reused for multiple requests. After completing all of your requests, call
    // the `certificateAuthorityServiceClient.close()` method on the client to safely
    // clean up any remaining background resources.

    CertificateAuthorityServiceClient.create().use { certificateAuthorityServiceClient ->

      // Set the Public Key and its format.
//      val cloudPublicKey: CloudPublicKey =
//        CloudPublicKey.newBuilder().setKey(publicKey).setFormat(KeyFormat.PEM).build()

      //*** NOT SURE IF THIS IS THE CORRECT WAY TO CONVERT ***
      val cloudPublicKey: CloudPublicKey = CloudPublicKey.parseFrom(publicKey.encoded)

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
            Duration.newBuilder().setSeconds(certificateLifetime).build()
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

      // Get the PEM encoded, signed X.509 certificate.
      logger.info(response.pemCertificate.toString())

      // To verify the obtained certificate, use this intermediate chain list.
      logger.info(response.pemCertificateChainList.toString())

      // ***  NOT SURE ON CORRECT INPUT TO CREATE PRIVATE KEY ***
//      val privateKey = generateKeyPair("TODO")

      //***  NOT SURE HOW CONVERT CERTIFICATE TO X509CERTIFICATE ***

//      return response.pemCertificate to privateKey

      TODO()
    }
  }

  companion object {
    private val logger by loggerFor()
  }
}
