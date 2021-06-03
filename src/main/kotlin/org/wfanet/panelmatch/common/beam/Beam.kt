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

package org.wfanet.panelmatch.common.beam

import org.apache.beam.sdk.transforms.Combine
import org.apache.beam.sdk.transforms.Count
import org.apache.beam.sdk.transforms.DoFn
import org.apache.beam.sdk.transforms.Flatten
import org.apache.beam.sdk.transforms.Keys
import org.apache.beam.sdk.transforms.ParDo
import org.apache.beam.sdk.transforms.Partition
import org.apache.beam.sdk.transforms.join.CoGroupByKey
import org.apache.beam.sdk.transforms.join.KeyedPCollectionTuple
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.apache.beam.sdk.values.PCollectionList
import org.apache.beam.sdk.values.PCollectionView
import org.apache.beam.sdk.values.TupleTag

infix fun <KeyT, ValueT> KeyT.toKv(value: ValueT): KV<KeyT, ValueT> {
  return KV.of(this, value)
}

fun <KeyT, ValueT> PCollection<KV<KeyT, ValueT>>.keys(): PCollection<KeyT> {
  return apply(Keys.create())
}

inline fun <InT, reified OutT> PCollection<InT>.parDo(
  crossinline block: suspend SequenceScope<OutT>.(InT) -> Unit
): PCollection<OutT> {
  return apply(
    ParDo.of(
      object : DoFn<InT, OutT>() {
        @ProcessElement
        fun processElement(@Element element: InT, output: OutputReceiver<OutT>) {
          sequence<OutT> { block(element) }.forEach(output::output)
        }
      }
    )
  )
}

inline fun <InT, reified OutT> PCollection<InT>.map(
  crossinline block: (InT) -> OutT
): PCollection<OutT> {
  return parDo { yield(block(it)) }
}

inline fun <InputT, reified KeyT> PCollection<InputT>.keyBy(
  crossinline keySelector: (InputT) -> KeyT
): PCollection<KV<KeyT, InputT>> {
  return map { keySelector(it) toKv it }
}

inline fun <InKeyT, reified OutKeyT, reified ValueT> PCollection<KV<InKeyT, ValueT>>.mapKeys(
  crossinline block: (InKeyT) -> OutKeyT
): PCollection<KV<OutKeyT, ValueT>> {
  return map { block(it.key) toKv it.value }
}

fun <T> PCollection<T>.partition(numParts: Int, block: (T) -> Int): PCollectionList<T> {
  return Partition.of(numParts) { value: T, _ -> block(value) }.expand(this)
}

inline fun <reified KeyT, reified Value1T, reified Value2T, reified OutT> PCollection<
  KV<KeyT, Value1T>>.join(
  right: PCollection<KV<KeyT, Value2T>>,
  crossinline transform:
    suspend SequenceScope<OutT>.(KeyT, Iterable<Value1T>, Iterable<Value2T>) -> Unit
): PCollection<OutT> {
  val leftTag = object : TupleTag<Value1T>() {}
  val rightTag = object : TupleTag<Value2T>() {}
  return KeyedPCollectionTuple.of(leftTag, this)
    .and(rightTag, right)
    .apply(CoGroupByKey.create())
    .parDo { transform(it.key, it.value.getAll(leftTag), it.value.getAll(rightTag)) }
}

fun <T> PCollection<T>.count(): PCollectionView<Long> {
  return Combine.globally<T, Long>(Count.combineFn()).asSingletonView().expand(this)
}

fun <T> Iterable<PCollection<T>>.toPCollectionList(): PCollectionList<T> {
  return PCollectionList.of(this)
}

fun <T> PCollectionList<T>.flatten(): PCollection<T> {
  return apply(Flatten.pCollections())
}

inline fun <InT, reified SideT, reified OutT> PCollection<InT>.parDoWithSideInput(
  sideInput: PCollectionView<SideT>,
  crossinline block: suspend SequenceScope<OutT>.(InT, SideT) -> Unit
): PCollection<OutT> {
  val doFn =
    object : DoFn<InT, OutT>() {
      @ProcessElement
      fun processElement(
        @Element element: InT,
        out: OutputReceiver<OutT>,
        context: ProcessContext
      ) {
        sequence<OutT> { block(element, context.sideInput(sideInput)) }.forEach(out::output)
      }
    }
  return ParDo.of(doFn).withSideInputs(sideInput).expand(this)
}
