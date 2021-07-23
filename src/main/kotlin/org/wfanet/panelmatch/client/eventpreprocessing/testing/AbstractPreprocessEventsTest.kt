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

package org.wfanet.panelmatch.client.eventpreprocessing.testing

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import java.lang.RuntimeException
import kotlin.test.assertFailsWith
import org.junit.Test
import org.wfanet.panelmatch.client.PreprocessEventsRequest
import org.wfanet.panelmatch.client.eventpreprocessing.PreprocessEvents

/** Abstract base class for testing implementations of [PreprocessEvents]. */
abstract class AbstractPreprocessEventsTest {
  abstract val preprocessEvents: PreprocessEvents

  @Test
  fun testPreprocessEvents() {
    val arbitraryId: Long = 1111
    val arbitraryData: ByteString = ByteString.copyFromUtf8("arbitrary-data")
    val arbitraryCryptoKey: ByteString = ByteString.copyFromUtf8("arbitrary-crypto-key")
    val arbitraryPepper: ByteString = ByteString.copyFromUtf8("arbitrary-pepper")

    val request =
      PreprocessEventsRequest.newBuilder()
        .apply {
          cryptoKey = arbitraryCryptoKey
          pepper = arbitraryPepper
          addUnprocessedEventsBuilder().apply {
            id = arbitraryId
            data = arbitraryData
          }
        }
        .build()
    val unImplementedException =
      assertFailsWith(RuntimeException::class) { preprocessEvents.preprocess(request) }

    assertThat(unImplementedException.message).contains("UNIMPLEMENTED: Not implemented")
  }
}
