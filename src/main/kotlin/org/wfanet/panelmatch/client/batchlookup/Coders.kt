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

package org.wfanet.panelmatch.client.batchlookup

import java.io.InputStream
import java.io.OutputStream
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.coders.Coder
import org.apache.beam.sdk.coders.CustomCoder
import org.apache.beam.sdk.coders.ListCoder
import org.apache.beam.sdk.coders.VarIntCoder
import org.apache.beam.sdk.coders.VarLongCoder
import org.apache.beam.sdk.extensions.protobuf.ByteStringCoder

/** Adds coders for types in this packages to the receiver's CoderRegistry. */
fun Pipeline.registerPirCoders() {
  val byteStringCoder = ByteStringCoder.of()
  val longCoder = VarLongCoder.of()
  val intCoder = VarIntCoder.of()

  val bucketIdCoder = addCoder(::BucketId, BucketId::id to intCoder)
  val shardIdCoder = addCoder(::ShardId, ShardId::id to intCoder)
  val queryIdCoder = addCoder(::QueryId, QueryId::id to intCoder)
  val bucketCoder =
    addCoder(::Bucket, Bucket::bucketId to bucketIdCoder, Bucket::data to byteStringCoder)

  addCoder(::DatabaseKey, DatabaseKey::id to longCoder)
  addCoder(::Plaintext, Plaintext::data to byteStringCoder)

  addCoder(
    ::QueryBundle,
    QueryBundle::shardId to shardIdCoder,
    QueryBundle::queryIds to ListCoder.of(queryIdCoder),
    QueryBundle::data to byteStringCoder
  )

  addCoder(
    ::DatabaseShard,
    DatabaseShard::shardId to shardIdCoder,
    DatabaseShard::buckets to ListCoder.of(bucketCoder)
  )

  addCoder(::Result, Result::queryId to queryIdCoder, Result::data to byteStringCoder)
}

private class ProxyCoder<T>(
  private val coders: List<T.(OutputStream) -> Unit>,
  private val decoder: (InputStream) -> T
) : CustomCoder<T>() {
  override fun encode(value: T, outStream: OutputStream) {
    for (encode in coders) {
      value.encode(outStream)
    }
  }

  override fun decode(inStream: InputStream): T {
    return decoder(inStream)
  }

  override fun verifyDeterministic() {}
}

private fun <T, U> bind(pair: Pair<U.() -> T, Coder<T>>): U.(OutputStream) -> Unit {
  val fn: U.() -> T = pair.first
  return { outputStream -> pair.second.encode(this.fn(), outputStream) }
}

private inline fun <reified T, V1> Pipeline.addCoder(
  noinline build: (V1) -> T,
  field1: Pair<T.() -> V1, Coder<V1>>
): Coder<T> {
  val coder = ProxyCoder(listOf(bind(field1))) { inStream -> build(field1.second.decode(inStream)) }
  coderRegistry.registerCoderForClass(T::class.java, coder)
  return coder
}

private inline fun <reified T, V1, V2> Pipeline.addCoder(
  noinline build: (V1, V2) -> T,
  field1: Pair<T.() -> V1, Coder<V1>>,
  field2: Pair<T.() -> V2, Coder<V2>>
): Coder<T> {
  val coder =
    ProxyCoder(listOf(bind(field1), bind(field2))) { inStream ->
      build(field1.second.decode(inStream), field2.second.decode(inStream))
    }
  coderRegistry.registerCoderForClass(T::class.java, coder)
  return coder
}

private inline fun <reified T, V1, V2, V3> Pipeline.addCoder(
  noinline build: (V1, V2, V3) -> T,
  field1: Pair<T.() -> V1, Coder<V1>>,
  field2: Pair<T.() -> V2, Coder<V2>>,
  field3: Pair<T.() -> V3, Coder<V3>>
): Coder<T> {
  val coder =
    ProxyCoder(listOf(bind(field1), bind(field2), bind(field3))) { inStream ->
      build(
        field1.second.decode(inStream),
        field2.second.decode(inStream),
        field3.second.decode(inStream)
      )
    }
  coderRegistry.registerCoderForClass(T::class.java, coder)
  return coder
}
