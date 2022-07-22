package org.wfanet.panelmatch.common.certificates.aws

import com.google.common.truth.Truth
import com.google.protobuf.kotlin.toByteStringUtf8
import java.io.ByteArrayOutputStream
import java.security.KeyPair
import java.security.PrivateKey
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wfanet.measurement.common.crypto.PemWriter
import org.wfanet.measurement.common.crypto.readCertificate
import org.wfanet.measurement.common.crypto.readPrivateKey
import org.wfanet.measurement.common.crypto.sign
import org.wfanet.measurement.common.crypto.testing.FIXED_CA_CERT_PEM_FILE
import org.wfanet.measurement.common.crypto.verifySignature
import org.wfanet.panelmatch.common.certificates.CertificateAuthority
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.acmpca.model.ASN1Subject
import software.amazon.awssdk.services.acmpca.model.ApiPassthrough
import software.amazon.awssdk.services.acmpca.model.ExtendedKeyUsage
import software.amazon.awssdk.services.acmpca.model.ExtendedKeyUsageType
import software.amazon.awssdk.services.acmpca.model.Extensions
import software.amazon.awssdk.services.acmpca.model.GeneralName
import software.amazon.awssdk.services.acmpca.model.GetCertificateRequest
import software.amazon.awssdk.services.acmpca.model.GetCertificateResponse
import software.amazon.awssdk.services.acmpca.model.IssueCertificateRequest
import software.amazon.awssdk.services.acmpca.model.IssueCertificateResponse
import software.amazon.awssdk.services.acmpca.model.KeyUsage
import software.amazon.awssdk.services.acmpca.model.SigningAlgorithm
import software.amazon.awssdk.services.acmpca.model.Validity

private val CONTEXT =
  CertificateAuthority.Context(
    commonName = "some-common-name",
    organization = "some-org-name",
    dnsName = "some-domain-name",
    validDays = 5,
  )

private const val CERTIFICATE_AUTHORITY_ARN = "some-ca-arn"
private const val CERTIFICATE_ARN = "some-cert-arn"
private val ROOT_X509 by lazy { readCertificate(FIXED_CA_CERT_PEM_FILE) }
private val ROOT_PUBLIC_KEY by lazy { ROOT_X509.publicKey }
private val ROOT_PRIVATE_KEY_FILE by lazy { FIXED_CA_CERT_PEM_FILE.resolveSibling("ca.key") }
private val CERTIFICATE_LIFETIME = Validity.builder().value(CONTEXT.validDays.toLong()).type("DAYS").build()

@RunWith(JUnit4::class)
class CertificateAuthorityTest {
  @Test
  fun generateX509CertificateAndPrivateKey() = runBlocking {
    val mockCreateCertificateClient: CreateCertificateClient = mock<CreateCertificateClient>()

    val certificateParams = ApiPassthrough.builder()
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
              .dnsName(CONTEXT.dnsName)
              .build())
          .build())
      .subject(
        ASN1Subject.builder()
          .commonName(CONTEXT.commonName)
          .organization(CONTEXT.organization)
          .build())
      .build()

    val expectedIssueCertificateRequest = IssueCertificateRequest.builder()
      .templateArn(AWS_CERTIFICATE_TEMPLATE_ARN)
      .apiPassthrough(certificateParams)
      .certificateAuthorityArn(CERTIFICATE_AUTHORITY_ARN)
      .csr(SdkBytes.fromByteArray(generateCsrFromPrivateKey(readPrivateKey(ROOT_PRIVATE_KEY_FILE, "ec")).toByteArray()))
      .signingAlgorithm(SigningAlgorithm.SHA256_WITHECDSA)
      .validity(CERTIFICATE_LIFETIME)
      .build()

    val expectedGetCertificateRequest = GetCertificateRequest.builder()
      .certificateArn(CERTIFICATE_ARN)
      .build()

    whenever(mockCreateCertificateClient.issueCertificate(any()))
      .thenReturn(
        IssueCertificateResponse.builder().certificateArn(CERTIFICATE_ARN).build()
      )

    whenever(mockCreateCertificateClient.getCertificate(any()))
      .thenReturn(
        GetCertificateResponse.builder().certificate(FIXED_CA_CERT_PEM_FILE.readText()).build()
      )

    val certificateAuthority = CertificateAuthority(
      CONTEXT,
      CERTIFICATE_AUTHORITY_ARN,
      mockCreateCertificateClient,
      generateKeyPair = { KeyPair(ROOT_PUBLIC_KEY, readPrivateKey(ROOT_PRIVATE_KEY_FILE, "ec")) }
    )

    val (x509, privateKey) = certificateAuthority.generateX509CertificateAndPrivateKey()

    argumentCaptor<IssueCertificateRequest> {
      verify(mockCreateCertificateClient).issueCertificate(capture())
      Truth.assertThat(firstValue).isEqualTo(expectedIssueCertificateRequest)
    }

    argumentCaptor<GetCertificateRequest> {
      verify(mockCreateCertificateClient).getCertificate(capture())
      Truth.assertThat(firstValue).isEqualTo(expectedGetCertificateRequest)
    }

    val data = "some-data-to-be-signed".toByteStringUtf8()
    val signature = privateKey.sign(x509, data)

    Truth.assertThat(x509.verifySignature(data, signature)).isTrue()
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
      "/O=${CONTEXT.organization}/CN=${CONTEXT.commonName}",
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
