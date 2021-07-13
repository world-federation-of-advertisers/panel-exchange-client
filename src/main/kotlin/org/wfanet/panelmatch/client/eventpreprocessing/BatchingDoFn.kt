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

import com.google.protobuf.BytesValue
import kotlin.reflect.KClass
import org.apache.beam.repackaged.core.org.apache.commons.lang3.ObjectUtils
import org.apache.beam.sdk.transforms.Combine
import org.apache.beam.sdk.transforms.Count
import org.apache.beam.sdk.transforms.DoFn
import org.apache.beam.sdk.transforms.Flatten
import org.apache.beam.sdk.transforms.Keys
import org.apache.beam.sdk.util.common.ElementByteSizeObserver;

import org.apache.beam.sdk.transforms.ParDo
import org.apache.beam.sdk.transforms.Partition
import org.apache.beam.sdk.transforms.SerializableFunction
import org.apache.beam.sdk.transforms.Values
import org.apache.beam.sdk.transforms.join.CoGroupByKey
import org.apache.beam.sdk.transforms.join.KeyedPCollectionTuple
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.apache.beam.sdk.values.PCollectionList
import org.apache.beam.sdk.values.PCollectionView
import org.apache.beam.sdk.values.TupleTag
import org.apache.beam.sdk.values.TypeDescriptor
import org.apache.beam.sdk.state.StateSpecs

import org.apache.beam.sdk.state.BagState

import org.apache.beam.sdk.state.StateSpec
import org.apache.beam.sdk.transforms.DoFn.StateId
import org.apache.beam.repackaged.core.org.apache.commons.lang3.ObjectUtils.firstNonNull

import org.apache.beam.sdk.state.ValueState

import org.apache.beam.sdk.transforms.DoFn.ProcessContext

import org.apache.beam.sdk.transforms.DoFn.ProcessElement
import org.apache.beam.sdk.transforms.windowing.GlobalWindow

import java.time.Instant
import org.apache.beam.sdk.transforms.DoFn.FinishBundle
import org.apache.beam.sdk.transforms.DoFn.FinishBundleContext


class BatchingDoFn<T>(
  private val maxByteSize: Long,
  private val getElementByteSize: SerializableFunction<T,Long>
): DoFn<T, List<T>>() {
  var buffer = mutableListOf<T>()
  var size: Long = 0L

  @ProcessElement
  fun process(
    element:T,
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
  //Do I Implement this here?
  private fun getElementByteSize(element: T): Byte {

  }

}


  @FinishBundle
  @Synchronized
  @Throws(Exception::class)
  fun finishBundle(
    context: FinishBundleContext
  ) {
    if (batchCells > 0) {
      //Confused what I am supposed to put here
    }
  }


