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
import org.apache.beam.sdk.transforms.DoFn.FinishBundle
import org.apache.beam.sdk.transforms.DoFn.FinishBundleContext
import org.apache.beam.sdk.transforms.DoFn.ProcessContext
import org.apache.beam.sdk.transforms.DoFn.ProcessElement
import org.apache.beam.sdk.transforms.SerializableFunction

class BatchingDoFn<T>(
  private val maxByteSize: Long,
  private val getElementByteSize: SerializableFunction<T, Long>
) : DoFn<T, List<T>>() {
  var buffer = mutableListOf<T>()
  var size: Long = 0L

  @ProcessElement
  fun process(
    element: T,
    context: ProcessContext,
  ) {
    size += getElementByteSize(element)
    if (size >= maxByteSize) {
      context.output(buffer)
      buffer.clear()
      size = 0
    }
    buffer.add(element)
  }
  // Do I Implement this here?
  private fun getElementByteSize(element: T): Byte {}
}

@FinishBundle
@Synchronized
@Throws(Exception::class)
fun finishBundle(context: FinishBundleContext) {
  if (batchCells > 0) {
    // Confused what I am supposed to put here
  }
}
