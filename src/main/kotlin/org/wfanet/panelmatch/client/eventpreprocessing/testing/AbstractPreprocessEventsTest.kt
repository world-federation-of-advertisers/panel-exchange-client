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
import org.wfanet.panelmatch.client.eventpreprocessing.PreprocessEvents
import wfanet.panelmatch.client.PreprocessEventsRequest

/** Abstract base class for testing implementations of [PreprocessEvents]. */
abstract class AbstractPreprocessEventsTest {
  abstract val PreprocessEvents: PreprocessEvents

  @Test
  fun testPreprocessEvents() {
    val randomID: ByteString = ByteString.copyFromUtf8("random-id")
    val randomData: ByteString = ByteString.copyFromUtf8("random-data")
    val randomCryptoKey: ByteString = ByteString.copyFromUtf8("random-crypto-key")
    val randomPepper: ByteString = ByteString.copyFromUtf8("random-pepper")

    val unImplementedException =
      assertFailsWith(RuntimeException::class) {
        val request = PreprocessEventsRequest.newBuilder().setCryptoKey(randomCryptoKey)
        request.setPepper(randomPepper)
        val unprocessedEvent = request.addUnprocessedEventsBuilder()
        unprocessedEvent.setId(randomID)
        unprocessedEvent.setData(randomData)
        PreprocessEvents.preprocess(request.build())
      }
    assertThat(unImplementedException.message).contains("UNIMPLEMENTED: Not implemented")
  }
}
