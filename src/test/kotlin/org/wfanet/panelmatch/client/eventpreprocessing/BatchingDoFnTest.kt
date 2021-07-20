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

import org.apache.beam.sdk.coders.StringUtf8Coder
import org.apache.beam.sdk.testing.TestStream
import org.apache.beam.sdk.transforms.DoFn
import org.apache.beam.sdk.transforms.ParDo
import org.apache.beam.sdk.transforms.SerializableFunction
import org.apache.beam.sdk.values.PCollection
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.panelmatch.client.eventpreprocessing.BatchingDoFn as BatchingDoFn
import org.wfanet.panelmatch.common.beam.testing.BeamTestBase
import org.wfanet.panelmatch.common.beam.testing.assertThat

/** Unit tests for [BatchingDoFn]. */
@RunWith(JUnit4::class)
class BatchingDoFnTest : BeamTestBase() {

  @Test
  fun testBatching() {
    val testStream1: TestStream<String> =
      TestStream.create(StringUtf8Coder.of())
        .addElements("1", "2", "3")
        .advanceWatermarkToInfinity()
    val batchSize1 = 1
    val doFn1: DoFn<String, MutableList<String>> = BatchingDoFn(batchSize1, StringLengthSize)
    var result1: PCollection<MutableList<String>>
    pipeline.apply(testStream1).apply { result1 = this.apply(ParDo.of(doFn1)) }
    pipeline.run()
    assertThat(result1)
      .containsInAnyOrder(mutableListOf("1"), mutableListOf("2"), mutableListOf("3"))
  }

  @Test
  fun testSingleBatch() {
    val testStream2: TestStream<String> =
      TestStream.create(StringUtf8Coder.of())
        .addElements("1", "2", "3")
        .advanceWatermarkToInfinity()
    val batchSize2 = 3
    val doFn2: DoFn<String, MutableList<String>> = BatchingDoFn(batchSize2, StringLengthSize)
    var result2: PCollection<MutableList<String>>
    pipeline.apply(testStream2).apply { result2 = this.apply(ParDo.of(doFn2)) }
    pipeline.run()
    assertThat(result2).containsInAnyOrder(mutableListOf("1", "2", "3"))
  }

  @Test
  fun testMaxByteSizeElement() {
    val testStream3: TestStream<String> =
      TestStream.create(StringUtf8Coder.of())
        .addElements("1", "234", "5")
        .advanceWatermarkToInfinity()
    val batchSize3 = 3
    val doFn3: DoFn<String, MutableList<String>> = BatchingDoFn(batchSize3, StringLengthSize)
    var result3: PCollection<MutableList<String>>
    pipeline.apply(testStream3).apply { result3 = this.apply(ParDo.of(doFn3)) }
    pipeline.run()
    assertThat(result3).containsInAnyOrder(mutableListOf("1", "5"), mutableListOf("234"))
  }

  @Test
  fun testExceedsMaxByteSizeElement() {
    val testStream4: TestStream<String> =
      TestStream.create(StringUtf8Coder.of())
        .addElements("1", "234", "5")
        .advanceWatermarkToInfinity()
    val batchSize4 = 2
    val doFn4: DoFn<String, MutableList<String>> = BatchingDoFn(batchSize4, StringLengthSize)
    var result4: PCollection<MutableList<String>>
    pipeline.apply(testStream4).apply { result4 = this.apply(ParDo.of(doFn4)) }
    pipeline.run()
    assertThat(result4).containsInAnyOrder(mutableListOf("1", "5"), mutableListOf("234"))
  }

  @Test
  fun emptyElements() {
    val testStream4: TestStream<String> =
      TestStream.create(StringUtf8Coder.of()).advanceWatermarkToInfinity()
    val batchSize4 = 3
    val doFn4: DoFn<String, MutableList<String>> = BatchingDoFn(batchSize4, StringLengthSize)
    var result4: PCollection<MutableList<String>>
    pipeline.apply(testStream4).apply { result4 = this.apply(ParDo.of(doFn4)) }
    pipeline.run()
    assertThat(result4).empty()
  }
}

private object StringLengthSize : SerializableFunction<String, Int> {
  override fun apply(s: String): Int {
    return s.length
  }
}
