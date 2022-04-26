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

import org.apache.beam.sdk.transforms.DoFn
import org.apache.beam.sdk.transforms.PTransform
import org.apache.beam.sdk.transforms.ParDo
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection

private const val PLACEHOLDER: Int = 42

/**
 * [PTransform] that breaks Dataflow fusion by forcing materialization of a [PCollection].
 *
 * This transform can be inserted between operations that have significantly different data sizes
 * or processing requirements to prevent Dataflow from fusing those operations together. Without
 * this, a heavy-processing stage that immediately follows a light-processing stage may be forced to
 * compute on a single worker. By inserting a [BreakFusion], the results of the light-processing
 * stage are materialized, allowing the heavy-processing stage to run with a different number of
 * workers.
 */
class BreakFusion<T> : PTransform<PCollection<T>, PCollection<T>>() {

  override fun expand(input: PCollection<T>): PCollection<T> {
    return input
      .apply("Pair With Placeholder", ParDo.of(PairWithPlaceholderFn()))
      .groupByKey()
      .keys()
  }

  private class PairWithPlaceholderFn<T> : DoFn<T, KV<T, Int>>() {
    @ProcessElement
    fun processElement(context: ProcessContext) {
      context.output(kvOf(context.element(), PLACEHOLDER))
    }
  }
}
