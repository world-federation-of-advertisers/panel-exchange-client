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
import kotlin.test.assertNotNull
import org.apache.beam.sdk.coders.Coder
import org.apache.beam.sdk.coders.KvCoder
import org.apache.beam.sdk.extensions.protobuf.ByteStringCoder
import org.apache.beam.sdk.values.KV
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.panelmatch.common.beam.kvOf
import org.wfanet.panelmatch.common.beam.testing.BeamTestBase

/** Unit tests for [eventPreprocessor]. */
@RunWith(JUnit4::class)
class EventPreprocessorTest : BeamTestBase() {
  @Test
  fun testEncrypt() {
    val coder: Coder<KV<ByteString, ByteString>> =
      KvCoder.of(ByteStringCoder.of(), ByteStringCoder.of())
    val collection =
      pcollectionOf("collection1", kvOf("A", "B"), kvOf("C", "D"), kvOf("C", "D"), coder = coder)
    val encrypted =
      eventPreprocessor(collection, 8, toByteString("pepper"), toByteString("cryptokey"))
    assertNotNull(encrypted)
  }

  fun kvOf(key: String, value: String): KV<ByteString, ByteString> {
    return kvOf(toByteString(key), toByteString(value))
  }
  fun toByteString(input: String): ByteString {
    return ByteString.copyFromUtf8(input)
  }
}
