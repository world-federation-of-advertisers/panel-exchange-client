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

import kotlin.reflect.KClass
import org.apache.beam.sdk.transforms.Combine
import org.apache.beam.sdk.transforms.Count
import org.apache.beam.sdk.transforms.DoFn
import org.apache.beam.sdk.transforms.Flatten
import org.apache.beam.sdk.transforms.Keys
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

/** Kotlin convenience helper for making [KV]s. */
fun <KeyT, ValueT> kvOf(key: KeyT, value: ValueT): KV<KeyT, ValueT> {
  return KV.of(key, value)
}

/** Returns the keys of a [PCollection] of [KV]s. */
fun <KeyT, ValueT> PCollection<KV<KeyT, ValueT>>.keys(name: String = "Keys"): PCollection<KeyT> {
  return apply(name, Keys.create())
}

/** Returns the values of a [PCollection] of [KV]s. */
fun <KeyT, ValueT> PCollection<KV<KeyT, ValueT>>.values(
  name: String = "Values"
): PCollection<ValueT> {
  return apply(name, Values.create())
}

/** Kotlin convenience helper for [ParDo]. */
inline fun <InT, reified OutT> PCollection<InT>.parDo(
  name: String = "ParDo",
  crossinline processElement: suspend SequenceScope<OutT>.(InT) -> Unit
): PCollection<OutT> {
  return apply(
    name,
    ParDo.of(
      object : DoFn<InT, OutT>() {
        @ProcessElement
        fun processElement(@Element element: InT, output: OutputReceiver<OutT>) {
          sequence<OutT> { processElement(element) }.forEach(output::output)
        }
      }
    )
  )
}

/** Kotlin convenience helper for a [ParDo] that has a single output per input. */
inline fun <InT, reified OutT> PCollection<InT>.map(
  name: String = "Map",
  crossinline processElement: (InT) -> OutT
): PCollection<OutT> {
  return parDo(name) { yield(processElement(it)) }
}

/** Kotlin convenience helper for a [ParDo] that yields an [Iterable] of outputs. */
inline fun <InT, reified OutT> PCollection<InT>.flatMap(
  name: String = "FlatMap",
  crossinline processElement: (InT) -> Iterable<OutT>
): PCollection<OutT> {
  return parDo(name) { yieldAll(processElement(it)) }
}

/** Kotlin convenience helper for keying a [PCollection] by some function of the inputs. */
inline fun <InputT, reified KeyT> PCollection<InputT>.keyBy(
  name: String = "KeyBy",
  crossinline keySelector: (InputT) -> KeyT
): PCollection<KV<KeyT, InputT>> {
  return map(name) { kvOf(keySelector(it), it) }
}

/** Kotlin convenience helper for transforming only the keys of a [PCollection] of [KV]s. */
inline fun <InKeyT, reified OutKeyT, reified ValueT> PCollection<KV<InKeyT, ValueT>>.mapKeys(
  name: String = "MapKeys",
  crossinline processKey: (InKeyT) -> OutKeyT
): PCollection<KV<OutKeyT, ValueT>> {
  return map(name) { kvOf(processKey(it.key), it.value) }
}

/** Kotlin convenience helper for transforming only the keys of a [PCollection] of [KV]s. */
inline fun <KeyT, reified InValueT, reified OutValueT> PCollection<KV<KeyT, InValueT>>.mapValues(
  name: String = "MapValues",
  crossinline processValue: (InValueT) -> OutValueT
): PCollection<KV<KeyT, OutValueT>> {
  return map(name) { kvOf(it.key, processValue(it.value)) }
}

/** Kotlin convenience helper for partitioning a [PCollection]. */
fun <T> PCollection<T>.partition(
  numParts: Int,
  name: String = "Partition",
  partitionBy: (T) -> Int
): PCollectionList<T> {
  return apply(name, Partition.of(numParts) { value: T, _ -> partitionBy(value) })
}

/** Kotlin convenience helper for a join between two [PCollection]s. */
inline fun <reified KeyT, reified Value1T, reified Value2T, reified OutT> PCollection<
  KV<KeyT, Value1T>>.join(
  right: PCollection<KV<KeyT, Value2T>>,
  name: String = "Join",
  crossinline transform:
    suspend SequenceScope<OutT>.(KeyT, Iterable<Value1T>, Iterable<Value2T>) -> Unit
): PCollection<OutT> {
  val leftTag = object : TupleTag<Value1T>() {}
  val rightTag = object : TupleTag<Value2T>() {}
  return KeyedPCollectionTuple.of(leftTag, this)
    .and(rightTag, right)
    .apply("$name/CoGroupByKey", CoGroupByKey.create())
    .parDo(name) { transform(it.key, it.value.getAll(leftTag), it.value.getAll(rightTag)) }
}

/** Kotlin convenience helper for getting the size of a [PCollection]. */
fun <T> PCollection<T>.count(name: String = "Count"): PCollectionView<Long> {
  return apply(name, Combine.globally<T, Long>(Count.combineFn()).asSingletonView())
}

/** Kotlin convenience helper for building a [PCollectionList]. */
fun <T> Iterable<PCollection<T>>.toPCollectionList(): PCollectionList<T> {
  return PCollectionList.of(this)
}

/** Kotlin convenience helper for flattening [PCollection]s. */
fun <T> PCollectionList<T>.flatten(name: String = "Flatten"): PCollection<T> {
  return apply(name, Flatten.pCollections())
}

/** Kotlin convenience helper for [parDo] but with a single side input. */
inline fun <InT, reified SideT, reified OutT> PCollection<InT>.parDoWithSideInput(
  sideInput: PCollectionView<SideT>,
  name: String = "ParDoWithSideInput",
  crossinline processElement: suspend SequenceScope<OutT>.(InT, SideT) -> Unit
): PCollection<OutT> {
  val doFn =
    object : DoFn<InT, OutT>() {
      @ProcessElement
      fun processElement(
        @Element element: InT,
        out: OutputReceiver<OutT>,
        context: ProcessContext
      ) {
        sequence<OutT> { processElement(element, context.sideInput(sideInput)) }
          .forEach(out::output)
      }
    }
  return apply(name, ParDo.of(doFn).withSideInputs(sideInput))
}

/** Groups receiver by key and then combines all values into one with [combiner]. */
inline fun <KeyT, reified ValueT> PCollection<KV<KeyT, ValueT>>.combinePerKey(
  name: String = "CombinePerKey",
  crossinline combiner: (Iterable<ValueT>) -> ValueT
): PCollection<KV<KeyT, ValueT>> {
  return apply(name, Combine.perKey<KeyT, ValueT>(SerializableFunction { combiner(it) }))
}

/** Convenient way to get a [TypeDescriptor] for the receiver. */
val <T : Any> KClass<T>.typeDescriptor: TypeDescriptor<T>
  get() = TypeDescriptor.of(this.java)