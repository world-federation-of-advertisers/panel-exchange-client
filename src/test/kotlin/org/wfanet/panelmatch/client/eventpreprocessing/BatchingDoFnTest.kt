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

  private val collection: PCollection<String> by lazy {
    pcollectionOf("collection1", "1", "2", "3")
  }
  private val collection2: PCollection<String> by lazy {
    pcollectionOf("collection12", "1", "2", "345")
  }
  @Test
  fun testBatching() {
    val batchSize: Int = 1
    val doFn: DoFn<String, MutableList<String>> = BatchingDoFn<String>(batchSize, StringLengthSize)
    val result: PCollection<MutableList<String>> = collection.apply(ParDo.of(doFn))
    assertThat(result)
      .containsInAnyOrder(mutableListOf("1"), mutableListOf("2"), mutableListOf("3"))
  }
}

private object StringLengthSize : SerializableFunction<String, Int> {
  override fun apply(s: String): Int {
    return s.length
  }
}
