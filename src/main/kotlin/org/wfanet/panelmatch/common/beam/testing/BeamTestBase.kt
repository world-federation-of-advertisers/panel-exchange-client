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

package org.wfanet.panelmatch.common.beam.testing

import org.apache.beam.sdk.testing.PAssert
import org.apache.beam.sdk.testing.TestPipeline
import org.apache.beam.sdk.transforms.Create
import org.apache.beam.sdk.transforms.DoFn
import org.apache.beam.sdk.transforms.ParDo
import org.apache.beam.sdk.values.PCollection
import org.apache.beam.sdk.values.PCollectionView
import org.junit.Rule

/**
 * Base class for Apache Beam tests.
 *
 * This makes a [TestPipeline] available to subclasses and sets it to run automatically in each test
 * case. It also provides a convenience function for building [PCollection]s.
 */
open class BeamTestBase {
  @get:Rule
  val pipeline: TestPipeline =
    TestPipeline.create().enableAbandonedNodeEnforcement(false).enableAutoRunIfMissing(true)

  protected fun <T> pcollectionOf(name: String, vararg values: T): PCollection<T> {
    return pipeline.apply(name, Create.of(values.asIterable()))
  }
}

fun <T> assertThat(pCollection: PCollection<T>): PAssert.IterableAssert<T> {
  return PAssert.that(pCollection)
}

inline fun <reified T> assertThat(view: PCollectionView<T>): PAssert.IterableAssert<T> {
  val parDo =
    object : DoFn<Int, T>() {
      @ProcessElement
      fun processElement(context: ProcessContext, out: OutputReceiver<T>) {
        out.output(context.sideInput(view))
      }
    }

  val extractedResult =
    view
      .pipeline
      .apply("Create Dummy PCollection", Create.of(1))
      .apply(ParDo.of(parDo).withSideInputs(view))

  return assertThat(extractedResult)
}
