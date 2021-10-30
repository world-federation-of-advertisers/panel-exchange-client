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
import org.junit.Test
import org.wfanet.panelmatch.client.PreprocessEventsRequestKt.unprocessedEvent
import org.wfanet.panelmatch.client.eventpreprocessing.EventPreprocessor
import org.wfanet.panelmatch.client.preprocessEventsRequest
import org.wfanet.panelmatch.common.compression.CompressionParametersKt.brotliCompressionParameters
import org.wfanet.panelmatch.common.compression.compressionParameters
import org.wfanet.panelmatch.common.toByteString

/** Abstract base class for testing implementations of [EventPreprocessor]. */
abstract class EventPreprocessorTest {
  abstract val eventPreprocessor: EventPreprocessor

  @Test
  fun testPreprocessEvents() {
    val request = preprocessEventsRequest {
      cryptoKey = "some-crypto-key".toByteString()
      identifierHashPepper = "some-identifier-hash-pepper".toByteString()
      hkdfPepper = "some-hkdf-pepper".toByteString()
      compressionParameters =
        compressionParameters {
          brotli = brotliCompressionParameters { dictionary = "some-dictionary".toByteString() }
        }
      unprocessedEvents +=
        unprocessedEvent {
          id = "some-id".toByteString()
          data = "some-data".toByteString()
        }
      unprocessedEvents +=
        unprocessedEvent {
          id = "some-other-id".toByteString()
          data = "some-other-data".toByteString()
        }
    }
    assertThat(eventPreprocessor.preprocess(request)).isNotNull()
  }
}
