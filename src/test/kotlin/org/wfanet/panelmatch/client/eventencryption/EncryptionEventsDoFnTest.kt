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

package org.wfanet.panelmatch.client.eventencryption

import com.google.protobuf.ByteString
import org.apache.beam.sdk.coders.KvCoder
import org.apache.beam.sdk.coders.ListCoder
import org.apache.beam.sdk.extensions.protobuf.ByteStringCoder
import org.apache.beam.sdk.transforms.DoFn
import org.apache.beam.sdk.transforms.ParDo
import org.apache.beam.sdk.transforms.SerializableFunction
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.panelmatch.client.PreprocessEventsRequest
import org.wfanet.panelmatch.client.PreprocessEventsResponse
import org.wfanet.panelmatch.client.eventencryption.EncryptionEventsDoFn as EncryptionEventsDoFn
import org.wfanet.panelmatch.common.beam.testing.BeamTestBase
import org.wfanet.panelmatch.common.beam.testing.assertThat

/** Unit tests for [EncryptionEventsDoFn]. */
@RunWith(JUnit4::class)
class EncryptionEventsDoFnTest : BeamTestBase() {
  val arbitraryUnprocessedEvents: MutableList<KV<ByteString, ByteString>> =
    mutableListOf(
      KV.of(ByteString.copyFromUtf8("1"), ByteString.copyFromUtf8("1")),
      KV.of(ByteString.copyFromUtf8("2"), ByteString.copyFromUtf8("2"))
    )

  private val collection: PCollection<MutableList<KV<ByteString, ByteString>>> by lazy {
    pcollectionOf(
      "collection1",
      arbitraryUnprocessedEvents,
      coder = ListCoder.of(KvCoder.of(ByteStringCoder.of(), ByteStringCoder.of()))
    )
  }

  @Test
  fun testEncrypt() {
    val doFn: DoFn<MutableList<KV<ByteString, ByteString>>, KV<ByteString, ByteString>> =
      EncryptionEventsDoFn(encryptEvents)
    val result: PCollection<KV<ByteString, ByteString>> = collection.apply(ParDo.of(doFn))
    assertThat(result)
      .containsInAnyOrder(
        KV.of(ByteString.copyFromUtf8("1abcdefg"), ByteString.copyFromUtf8("1hijklmnop")),
        KV.of(ByteString.copyFromUtf8("2abcdefg"), ByteString.copyFromUtf8("2hijklmnop"))
      )
  }
}

private object encryptEvents :
  SerializableFunction<PreprocessEventsRequest, PreprocessEventsResponse> {
  override fun apply(request: PreprocessEventsRequest): PreprocessEventsResponse {
    val response =
      PreprocessEventsResponse.newBuilder()
        .apply {
          for (events in request.unprocessedEventsList) {
            addProcessedEventsBuilder().apply {
              this.setEncryptedId(events.id.concat(ByteString.copyFromUtf8("abcdefg")))
              this.setEncryptedData(events.data.concat(ByteString.copyFromUtf8("hijklmnop")))
            }
          }
        }
        .build()
    return response
  }
}
