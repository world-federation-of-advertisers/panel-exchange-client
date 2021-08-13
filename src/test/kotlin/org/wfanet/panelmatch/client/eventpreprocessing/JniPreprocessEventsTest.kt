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

package org.wfanet.panelmatch.client.eventpreprocessing

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.panelmatch.client.PreprocessEventsRequest
import org.wfanet.panelmatch.client.eventpreprocessing.testing.AbstractPreprocessEventsTest
import org.wfanet.panelmatch.common.JniException

@RunWith(JUnit4::class)
class JniPreprocessEventsTest : AbstractPreprocessEventsTest() {
  override val preprocessEvents: PreprocessEvents = JniPreprocessEvents()
  @Test
  fun testJniWrapExceptionIdentifierHashPepper() {
    val request =
      PreprocessEventsRequest.newBuilder()
        .apply {
          cryptoKey = ByteString.copyFromUtf8("arbitrary-cryptokey")
          hkdfPepper = ByteString.copyFromUtf8("arbitrary-hkdf-pepper")
          addUnprocessedEventsBuilder().apply {
            id = ByteString.copyFromUtf8("arbitrary-id")
            data = ByteString.copyFromUtf8("arbitrary-data")
          }
        }
        .build()
    val noPepper = assertFailsWith(JniException::class) { preprocessEvents.preprocess(request) }
    assertThat(noPepper.message).contains("INVALID ARGUMENT: Empty Identifier Hash Pepper")
  }
  @Test
  fun testJniWrapExceptionHkdfPepper() {
    val request =
      PreprocessEventsRequest.newBuilder()
        .apply {
          cryptoKey = ByteString.copyFromUtf8("arbitrary-cryptokey")
          identifierHashPepper = ByteString.copyFromUtf8("arbitrary-identifier-hash-pepper")
          addUnprocessedEventsBuilder().apply {
            id = ByteString.copyFromUtf8("arbitrary-id")
            data = ByteString.copyFromUtf8("arbitrary-data")
          }
        }
        .build()
    val noPepper = assertFailsWith(JniException::class) { preprocessEvents.preprocess(request) }
    assertThat(noPepper.message).contains("INVALID ARGUMENT: Empty HKDF Pepper")
  }
  @Test
  fun testJniWrapExceptionCryptoKey() {
    assertFailsWith(JniException::class) {
      val request =
        PreprocessEventsRequest.newBuilder()
          .apply {
            identifierHashPepper = ByteString.copyFromUtf8("arbitrary-identifier-hash-pepper")
            hkdfPepper = ByteString.copyFromUtf8("arbitrary-hkdf-pepper")
            addUnprocessedEventsBuilder().apply {
              id = ByteString.copyFromUtf8("arbitrary-id")
              data = ByteString.copyFromUtf8("arbitrary-data")
            }
          }
          .build()
      preprocessEvents.preprocess(request)
    }
  }
}
