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
  fun testJniWrapExceptionPepper() {
    val noPepper =
      assertFailsWith(JniException::class) {
        val request =
          PreprocessEventsRequest.newBuilder()
            .apply {
              cryptoKey = ByteString.copyFromUtf8("arbitrary-cryptokey")
              addUnprocessedEventsBuilder().apply {
                id = ByteString.copyFromUtf8("arbitrary-id")
                data = ByteString.copyFromUtf8("arbitrary-data")
              }
            }
            .build()
        preprocessEvents.preprocess(request)
      }
    assertThat(noPepper.message).contains("INVALID ARGUMENT: Empty Pepper")
  }
  @Test
  fun testJniWrapExceptionCryptoKey() {
    assertFailsWith(JniException::class) {
      val request =
        PreprocessEventsRequest.newBuilder()
          .apply {
            pepper = ByteString.copyFromUtf8("arbitrary-pepper")
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
