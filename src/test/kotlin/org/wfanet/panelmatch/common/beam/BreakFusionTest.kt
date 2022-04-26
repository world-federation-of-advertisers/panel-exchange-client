// Copyright 2022 The Cross-Media Measurement Authors
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

package org.wfanet.panelmatch.common.beam

import org.apache.beam.sdk.values.PCollection
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.panelmatch.common.beam.testing.BeamTestBase
import org.wfanet.panelmatch.common.beam.testing.assertThat

@RunWith(JUnit4::class)
class BreakFusionTest : BeamTestBase() {

  private val collection: PCollection<String> by lazy {
    pcollectionOf("collection", "cat", "dog", "fox")
  }

  @Test
  fun breakFusionReturnsOriginalPCollection() {
    val result: PCollection<String> = collection.apply("Break Fusion", BreakFusion())

    assertThat(result).containsInAnyOrder("cat", "dog", "fox")
  }
}
