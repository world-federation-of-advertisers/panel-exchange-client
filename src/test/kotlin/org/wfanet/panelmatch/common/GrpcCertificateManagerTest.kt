package org.wfanet.panelmatch.common

import org.junit.Before
import org.junit.Rule
import org.mockito.kotlin.UseConstructor
import org.mockito.kotlin.mock
import org.wfanet.measurement.common.grpc.testing.GrpcTestServerRule
import org.wfanet.measurement.internal.kingdom.CertificatesGrpcKt.CertificatesCoroutineImplBase
import org.wfanet.measurement.internal.kingdom.CertificatesGrpcKt.CertificatesCoroutineStub

private val internalCertificatesMock: CertificatesCoroutineImplBase =
  mock(useConstructor = UseConstructor.parameterless()) {
    onBlocking { getCertificate(any()) }.thenReturn(INTERNAL_CERTIFICATE)
    onBlocking { createCertificate(any()) }.thenReturn(INTERNAL_CERTIFICATE)
    onBlocking { revokeCertificate(any()) }.thenReturn(INTERNAL_CERTIFICATE)
    onBlocking { releaseCertificateHold(any()) }.thenReturn(INTERNAL_CERTIFICATE)
  }

@get:Rule val grpcTestServerRule = GrpcTestServerRule { addService(internalCertificatesMock) }

private lateinit var service: CertificatesService

@Before
fun initService() {
  service = CertificatesService(CertificatesCoroutineStub(grpcTestServerRule.channel))
}
