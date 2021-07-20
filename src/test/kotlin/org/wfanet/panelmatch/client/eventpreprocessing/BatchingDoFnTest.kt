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
    val result = makeSingleBundleParDoDoFn(1, "1", "2", "3")
    assertThat(result)
      .containsInAnyOrder(mutableListOf("1"), mutableListOf("2"), mutableListOf("3"))
  }

  @Test
  fun testSingleBatch() {
    val result = makeSingleBundleParDoDoFn(3, "1", "2", "3")
    assertThat(result).containsInAnyOrder(mutableListOf("1", "2", "3"))
  }

  @Test
  fun testMaxByteSizeElement() {
    val result = makeSingleBundleParDoDoFn(3, "1", "234", "5")
    assertThat(result).containsInAnyOrder(mutableListOf("1", "5"), mutableListOf("234"))
  }

  @Test
  fun testExceedsMaxByteSizeElement() {
    val result = makeSingleBundleParDoDoFn(2, "1", "234", "5")
    assertThat(result).containsInAnyOrder(mutableListOf("1", "5"), mutableListOf("234"))
  }

  @Test
  fun emptyElements() {
    val result = makeEmptyBundleParDoDoFn(2)
    assertThat(result).empty()
  }

  @Test
  fun multipleBundles() {
    val result1 = makeSingleBundleParDoDoFn(2, "1", "23", "1")
    val result2 = makeEmptyBundleParDoDoFn(4)
    val result3 = makeSingleBundleParDoDoFn(5, "1", "234", "5")
    assertThat(result1).containsInAnyOrder(mutableListOf("1", "1"), mutableListOf("23"))
    assertThat(result2).empty()
    assertThat(result3).containsInAnyOrder(mutableListOf("1", "234", "5"))
  }

  private fun makeSingleBundleParDoDoFn(
    batchSize: Int,
    item: String,
    vararg items: String
  ): PCollection<MutableList<String>> {
    val testStream =
      TestStream.create(StringUtf8Coder.of()).addElements(item, *items).advanceWatermarkToInfinity()
    return pipeline.apply(testStream).apply(ParDo.of(BatchingDoFn(batchSize, StringLengthSize)))
  }
  private fun makeEmptyBundleParDoDoFn(batchSize: Int): PCollection<MutableList<String>> {
    val testStream = TestStream.create(StringUtf8Coder.of()).advanceWatermarkToInfinity()
    return pipeline.apply(testStream).apply(ParDo.of(BatchingDoFn(batchSize, StringLengthSize)))
  }
}

private object StringLengthSize : SerializableFunction<String, Int> {
  override fun apply(s: String): Int {
    return s.length
  }
}
