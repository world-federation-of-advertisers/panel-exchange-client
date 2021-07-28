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

import com.google.protobuf.ByteString
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import org.apache.beam.sdk.coders.Coder
import org.apache.beam.sdk.coders.KvCoder
import org.apache.beam.sdk.extensions.protobuf.ByteStringCoder
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.panelmatch.common.beam.kvOf
import org.wfanet.panelmatch.common.beam.testing.BeamTestBase

/** Unit tests for [preprocessEventsInPipeline]. */
@RunWith(JUnit4::class)
class PreprocessEventsInPipelineTest : BeamTestBase() {
  @Test
  fun testEncryptByteStrings() {

    val events = eventsOf("A" to "B", "C" to "D")
    val encrypted =
      preprocessEventsInPipeline(events, 8, "pepper".toByteString(), "cryptokey".toByteString())
    assertNotNull(encrypted)
    assertNotEquals(events, encrypted)
  }
  @Test
  fun testEncryptSerializableFunctions() {

    val events = eventsOf("A" to "B", "C" to "D")
    val encrypted =
      preprocessEventsInPipeline(
        events,
        8,
        HardCodedPepperProvider("pepper".toByteString()),
        HardCodedPepperProvider("cryptokey".toByteString())
      )
    assertNotNull(encrypted)
    assertNotEquals(events, encrypted)
  }

  fun eventsOf(vararg pairs: Pair<String, String>): PCollection<KV<ByteString, ByteString>> {
    val coder: Coder<KV<ByteString, ByteString>> =
      KvCoder.of(ByteStringCoder.of(), ByteStringCoder.of())
    return pcollectionOf(
      "Create Events",
      *pairs.map { kvOf(it.first.toByteString(), it.second.toByteString()) }.toTypedArray(),
      coder = coder
    )
  }
  private fun String.toByteString(): ByteString {
    return ByteString.copyFromUtf8(this)
  }
}
