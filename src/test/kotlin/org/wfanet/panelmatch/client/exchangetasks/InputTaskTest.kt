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

package org.wfanet.panelmatch.client.exchangetasks

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import java.lang.IllegalArgumentException
import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.Flow
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow.Step.StepCase.INPUT_STEP
import org.wfanet.measurement.common.asBufferedFlow
import org.wfanet.measurement.common.crypto.readCertificate
import org.wfanet.measurement.common.crypto.readPrivateKey
import org.wfanet.measurement.common.crypto.testing.FIXED_SERVER_CERT_PEM_FILE
import org.wfanet.measurement.common.crypto.testing.FIXED_SERVER_KEY_FILE
import org.wfanet.measurement.common.crypto.testing.KEY_ALGORITHM
import org.wfanet.measurement.common.throttler.Throttler
import org.wfanet.measurement.storage.StorageClient
import org.wfanet.panelmatch.client.launcher.testing.MP_0_SECRET_KEY
import org.wfanet.panelmatch.client.launcher.testing.buildStep
import org.wfanet.panelmatch.client.storage.VerifiedStorageClient
import org.wfanet.panelmatch.common.testing.runBlockingTest

@RunWith(JUnit4::class)
class InputTaskTest {
  private val underlyingPrivateStorage = mock<StorageClient>()
  private val underlyingSharedStorage = mock<StorageClient>()
  private val privateStorage =
    VerifiedStorageClient(
      underlyingPrivateStorage,
      readCertificate(FIXED_SERVER_CERT_PEM_FILE),
      readCertificate(FIXED_SERVER_CERT_PEM_FILE),
      readPrivateKey(FIXED_SERVER_KEY_FILE, KEY_ALGORITHM)
    )
  private val sharedStorage =
    VerifiedStorageClient(
      underlyingSharedStorage,
      readCertificate(FIXED_SERVER_CERT_PEM_FILE),
      readCertificate(FIXED_SERVER_CERT_PEM_FILE),
      readPrivateKey(FIXED_SERVER_KEY_FILE, KEY_ALGORITHM)
    )
  private val secretKeySourceBlob =
    mock<StorageClient.Blob> {
      on { read(any()) } doReturn MP_0_SECRET_KEY.asBufferedFlow(1024)
    } // MP_0_SECRET_KEY

  private val throttler =
    object : Throttler {
      override suspend fun <T> onReady(block: suspend () -> T): T {
        return block()
      }
    }

  @Test
  fun `wait on private input`() = runBlockingTest {
    val labels = mapOf("input" to "mp-crypto-key")
    val step = buildStep(INPUT_STEP, privateOutputLabels = labels)
    val task = InputTask(step, throttler, sharedStorage, privateStorage)

    whenever(underlyingPrivateStorage.getBlob("mp-crypto-key"))
      .thenReturn(null)
      .thenReturn(null)
      .thenReturn(null)
      .thenReturn(null)
      .thenReturn(secretKeySourceBlob)

    val result: Map<String, Flow<ByteString>> = task.execute(emptyMap())

    assertThat(result).isEmpty()

    verify(underlyingPrivateStorage, times(5)).getBlob("mp-crypto-key")
    verify(underlyingPrivateStorage, times(1)).defaultBufferSizeBytes
    verify(underlyingSharedStorage, times(1)).defaultBufferSizeBytes
    verifyNoMoreInteractions(underlyingSharedStorage, underlyingPrivateStorage)
  }

  @Test
  fun `wait on shared input`() = runBlockingTest {
    val labels = mapOf("input" to "mp-crypto-key")
    val step = buildStep(INPUT_STEP, sharedOutputLabels = labels)
    val task = InputTask(step, throttler, sharedStorage, privateStorage)

    whenever(underlyingSharedStorage.getBlob("mp-crypto-key"))
      .thenReturn(null)
      .thenReturn(null)
      .thenReturn(null)
      .thenReturn(null)
      .thenReturn(secretKeySourceBlob)

    val result: Map<String, Flow<ByteString>> = task.execute(emptyMap())

    assertThat(result).isEmpty()

    verify(underlyingSharedStorage, times(5)).getBlob("mp-crypto-key")
    verify(underlyingPrivateStorage, times(1)).defaultBufferSizeBytes
    verify(underlyingSharedStorage, times(1)).defaultBufferSizeBytes

    verifyNoMoreInteractions(underlyingSharedStorage, underlyingPrivateStorage)
  }

  @Test
  fun `invalid inputs`() = runBlockingTest {
    fun runTest(step: ExchangeWorkflow.Step) {
      if (step.privateInputLabelsCount + step.sharedInputLabelsCount == 0 &&
          step.privateOutputLabelsCount + step.sharedOutputLabelsCount == 1
      ) {
        // Expect no failure.
        InputTask(step, throttler, sharedStorage, privateStorage)
      } else {
        assertFailsWith<IllegalArgumentException>(step.toString()) {
          InputTask(step, throttler, sharedStorage, privateStorage)
        }
      }
    }

    val maps: List<Map<String, String>> =
      listOf(emptyMap(), mapOf("a" to "b"), mapOf("a" to "b", "c" to "d"))

    for (privateInputLabels in maps) {
      for (privateOutputLabels in maps) {
        for (sharedInputLabels in maps) {
          for (sharedOutputLabels in maps) {
            runTest(
              buildStep(
                stepType = INPUT_STEP,
                privateInputLabels = privateInputLabels,
                privateOutputLabels = privateOutputLabels,
                sharedInputLabels = sharedInputLabels,
                sharedOutputLabels = sharedOutputLabels
              )
            )
          }
        }
      }
    }
  }
}
