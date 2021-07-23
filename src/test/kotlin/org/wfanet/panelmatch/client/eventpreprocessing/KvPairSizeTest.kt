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
import org.apache.beam.sdk.values.KV
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class KvPairSizeTest {
  val longBytes = Long.SIZE_BYTES
  @Test
  fun emptyByteString() {
    val test: KV<Long, ByteString> = KV.of(100, ByteString.EMPTY)
    val result: Int = KvPairSize.apply(test)
    assertThat(result).isEqualTo(longBytes)
  }
  fun string() {
    val test: KV<Long, ByteString> = longByteStringKvOf(200, "12345")
    val result: Int = KvPairSize.apply(test)
    assertThat(result).isEqualTo(5 + longBytes)
  }
  fun stringSpaces() {
    val test: KV<Long, ByteString> = longByteStringKvOf(300, "12345 789")
    val result: Int = KvPairSize.apply(test)
    assertThat(result).isEqualTo(9 + longBytes)
  }
  fun longByteStringKvOf(key: Long, value: String): KV<Long, ByteString> {
    return KV.of(key, ByteString.copyFromUtf8(value))
  }
}
