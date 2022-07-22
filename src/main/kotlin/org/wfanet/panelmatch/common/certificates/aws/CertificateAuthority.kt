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

package org.wfanet.panelmatch.common.certificates.aws

import java.io.ByteArrayOutputStream
import java.security.KeyPair
import java.security.PrivateKey
import java.security.cert.X509Certificate
import org.wfanet.measurement.common.crypto.PemWriter
import org.wfanet.measurement.common.crypto.generateKeyPair
import org.wfanet.measurement.common.crypto.readCertificate
import org.wfanet.panelmatch.common.certificates.CertificateAuthority
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.acmpca.model.ASN1Subject
import software.amazon.awssdk.services.acmpca.model.ApiPassthrough
import software.amazon.awssdk.services.acmpca.model.ExtendedKeyUsage
import software.amazon.awssdk.services.acmpca.model.ExtendedKeyUsageType
import software.amazon.awssdk.services.acmpca.model.Extensions
import software.amazon.awssdk.services.acmpca.model.GeneralName
import software.amazon.awssdk.services.acmpca.model.GetCertificateRequest
import software.amazon.awssdk.services.acmpca.model.IssueCertificateRequest
import software.amazon.awssdk.services.acmpca.model.KeyUsage
import software.amazon.awssdk.services.acmpca.model.SigningAlgorithm
import software.amazon.awssdk.services.acmpca.model.Validity


/**
 * Defines the template ARN used by AWS private CA to issue certificate.
 *
 * More information: https://docs.aws.amazon.com/acm-pca/latest/userguide/UsingTemplates.html#BlankSubordinateCACertificate_PathLen0_APIPassthrough
 */
const val AWS_CERTIFICATE_TEMPLATE_ARN = "arn:aws:acm-pca:::template/BlankSubordinateCACertificate_PathLen0_APIPassthrough/V1"

class CertificateAuthority(
  private val context: CertificateAuthority.Context,
  private val certificateAuthorityArn: String,
  private val client: CreateCertificateClient,
  private val generateKeyPair: () -> KeyPair = { generateKeyPair("EC") }
) : CertificateAuthority {

  private val certificateParams = ApiPassthrough.builder()
    .extensions(
      Extensions.builder()
        .keyUsage(
          KeyUsage.builder()
            .digitalSignature(true)
            .keyEncipherment(true)
            .keyCertSign(true)
            .build())
        .extendedKeyUsage(
          ExtendedKeyUsage.builder()
            .extendedKeyUsageType(ExtendedKeyUsageType.SERVER_AUTH)
            .build())
        .subjectAlternativeNames(
          GeneralName.builder()
            .dnsName(context.dnsName)
            .build())
        .build())
    .subject(
      ASN1Subject.builder()
        .commonName(context.commonName)
        .organization(context.organization)
        .build())
    .build()

  private val certificateLifetime = Validity.builder().value(context.validDays.toLong()).type("DAYS").build()

  override suspend fun generateX509CertificateAndPrivateKey(): Pair<X509Certificate, PrivateKey> {
    val keyPair: KeyPair = generateKeyPair()
    val privateKey: PrivateKey = keyPair.private

    val issueRequest = IssueCertificateRequest.builder()
      .templateArn(AWS_CERTIFICATE_TEMPLATE_ARN)
      .apiPassthrough(certificateParams)
      .certificateAuthorityArn(certificateAuthorityArn)
      .csr(SdkBytes.fromByteArray(generateCsrFromPrivateKey(privateKey).toByteArray()))
      .signingAlgorithm(SigningAlgorithm.SHA256_WITHECDSA)
      .validity(certificateLifetime)
      .build()

    val issueResponse = client.issueCertificate(issueRequest)

    val certificateArn = issueResponse.certificateArn()

    val getRequest = GetCertificateRequest.builder()
      .certificateArn(certificateArn)
      .certificateAuthorityArn(certificateAuthorityArn)
      .build()

    val getResponse = client.getCertificate(getRequest)

    return readCertificate(getResponse.certificate().byteInputStream()) to privateKey
  }

  private fun generateCsrFromPrivateKey(key: PrivateKey) : String {
    val outputStream = ByteArrayOutputStream()
    PemWriter(outputStream).write(key)
    return runProcessWithInputAndReturnOutput(
      outputStream.toByteArray(),
      "openssl",
      "req",
      "-new",
      "-subj",
      "/O=${context.organization}/CN=${context.commonName}",
      "-key",
      "/dev/stdin"
    )
  }

  private fun runProcessWithInputAndReturnOutput(input: ByteArray, vararg args: String) : String {
    val process = ProcessBuilder(*args).redirectErrorStream(true).start()
    process.outputStream.write(input)
    process.outputStream.close()
    val exitCode = process.waitFor()
    val output = process.inputStream.use { it.bufferedReader().readText() }
    check(exitCode == 0) {
      "Command ${args.joinToString(" ")} failed with code $exitCode. Output:\n$output"
    }
    return output
  }
}
