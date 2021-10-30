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
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.panelmatch.client.PreprocessEventsRequestKt.unprocessedEvent
import org.wfanet.panelmatch.client.copy
import org.wfanet.panelmatch.client.eventpreprocessing.testing.EventPreprocessorTest
import org.wfanet.panelmatch.client.preprocessEventsRequest
import org.wfanet.panelmatch.common.JniException
import org.wfanet.panelmatch.common.compression.CompressionParametersKt.noCompression
import org.wfanet.panelmatch.common.compression.compressionParameters
import org.wfanet.panelmatch.common.toByteString

private val REQUEST = preprocessEventsRequest {
  cryptoKey = "some-crypto-key".toByteString()
  hkdfPepper = "some-hkdf-pepper".toByteString()
  identifierHashPepper = "some-identifier-hash-pepper".toByteString()
  compressionParameters = compressionParameters { uncompressed = noCompression {} }
  unprocessedEvents +=
    unprocessedEvent {
      id = "some-id".toByteString()
      data = "some-data".toByteString()
    }
}

@RunWith(JUnit4::class)
class JniEventPreprocessorTest : EventPreprocessorTest() {
  override val eventPreprocessor: EventPreprocessor = JniEventPreprocessor()

  @Test
  fun missingIdentifierHashPepper() {
    val request = REQUEST.copy { clearIdentifierHashPepper() }
    val e = assertFailsWith(JniException::class) { eventPreprocessor.preprocess(request) }
    assertThat(e).hasMessageThat().contains("Empty Identifier Hash Pepper")
  }

  @Test
  fun missingHkdfPepper() {
    val request = REQUEST.copy { clearHkdfPepper() }
    val e = assertFailsWith<JniException> { eventPreprocessor.preprocess(request) }
    assertThat(e).hasMessageThat().contains("Empty HKDF Pepper")
  }

  @Test
  fun missingCryptoKey() {
    val request = REQUEST.copy { clearCryptoKey() }
    assertFailsWith<JniException> { eventPreprocessor.preprocess(request) }
  }

  @Test
  fun missingCompressionParameters() {
    val request = REQUEST.copy { clearCompressionParameters() }
    assertFailsWith<JniException> { eventPreprocessor.preprocess(request) }
  }
}
