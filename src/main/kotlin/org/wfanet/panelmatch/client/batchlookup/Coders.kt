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
import java.io.Serializable
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.coders.Coder
import org.apache.beam.sdk.coders.CustomCoder
import org.apache.beam.sdk.coders.ListCoder
import org.apache.beam.sdk.coders.VarIntCoder
import org.apache.beam.sdk.coders.VarLongCoder
import org.apache.beam.sdk.extensions.protobuf.ByteStringCoder

/** Adds coders for types in this package to the receiver's CoderRegistry. */
fun Pipeline.registerPirCoders() {
  val byteStringCoder = ByteStringCoder.of()
  val longCoder = VarLongCoder.of()
  val intCoder = VarIntCoder.of()

  val bucketIdCoder = addCoder(::BucketId, PropertyCoder(BucketId::id, intCoder))
  val shardIdCoder = addCoder(::ShardId, PropertyCoder(ShardId::id, intCoder))
  val queryIdCoder = addCoder(::QueryId, PropertyCoder(QueryId::id, intCoder))
  val bucketCoder =
    addCoder(
      ::Bucket,
      PropertyCoder(Bucket::bucketId, bucketIdCoder),
      PropertyCoder(Bucket::data, byteStringCoder)
    )
  val queryMetadataCoder =
    addCoder(
      ::QueryMetadata,
      PropertyCoder(QueryMetadata::queryId, queryIdCoder),
      PropertyCoder(QueryMetadata::metadata, byteStringCoder)
    )

  addCoder(::DatabaseKey, PropertyCoder(DatabaseKey::id, longCoder))
  addCoder(::Plaintext, PropertyCoder(Plaintext::data, byteStringCoder))

  addCoder(
    ::QueryBundle,
    PropertyCoder(QueryBundle::shardId, shardIdCoder),
    PropertyCoder(QueryBundle::queryMetadata, ListCoder.of(queryMetadataCoder)),
    PropertyCoder(QueryBundle::data, byteStringCoder)
  )

  addCoder(
    ::DatabaseShard,
    PropertyCoder(DatabaseShard::shardId, shardIdCoder),
    PropertyCoder(DatabaseShard::buckets, ListCoder.of(bucketCoder))
  )

  addCoder(
    ::Result,
    PropertyCoder(Result::queryMetadata, queryMetadataCoder),
    PropertyCoder(Result::data, byteStringCoder)
  )
}

/** A deterministic [Coder] built from a list of [PropertyCoder]s and a single decoding function. */
private class ProxyCoder<T>(
  private val encoders: List<PropertyCoder<*, T>>,
  private val decoder: (InputStream) -> T
) : CustomCoder<T>() {
  override fun encode(value: T, outStream: OutputStream) {
    for (propertyCoder in encoders) {
      propertyCoder.encode(value, outStream)
    }
  }

  override fun decode(inStream: InputStream): T {
    return decoder(inStream)
  }

  override fun verifyDeterministic() {}
}

/** [Coder]-like class for handling [T]-typed properties of [U]. */
private class PropertyCoder<T, U>(private val extract: U.() -> T, private val coder: Coder<T>) :
  Serializable {
  /** Encodes the [T] derived from [value] by [extract]. */
  fun encode(value: U, outStream: OutputStream) {
    coder.encode(value.extract(), outStream)
  }

  /** Decodes a [T] from [inStream]. */
  fun decode(inStream: InputStream): T {
    return coder.decode(inStream)
  }
}

/** Builds and registers a [ProxyCoder]. */
private inline fun <reified T> Pipeline.addProxyCoder(
  vararg encoders: PropertyCoder<*, T>,
  crossinline decoder: (InputStream) -> T
): Coder<T> {
  val coder = ProxyCoder(encoders.toList()) { decoder(it) }
  coderRegistry.registerCoderForClass(T::class.java, coder)
  return coder
}

/** Builds and registers a [ProxyCoder] for a single-property class. */
private inline fun <reified T, V1> Pipeline.addCoder(
  noinline build: (V1) -> T,
  field1: PropertyCoder<V1, T>
): Coder<T> {
  return addProxyCoder(field1) { inStream -> build(field1.decode(inStream)) }
}

/** Builds and registers a [ProxyCoder] for a two-property class. */
private inline fun <reified T, V1, V2> Pipeline.addCoder(
  noinline build: (V1, V2) -> T,
  field1: PropertyCoder<V1, T>,
  field2: PropertyCoder<V2, T>
): Coder<T> {
  return addProxyCoder(field1, field2) { inStream ->
    build(field1.decode(inStream), field2.decode(inStream))
  }
}

/** Builds and registers a [ProxyCoder] for a three-property class. */
private inline fun <reified T, V1, V2, V3> Pipeline.addCoder(
  noinline build: (V1, V2, V3) -> T,
  field1: PropertyCoder<V1, T>,
  field2: PropertyCoder<V2, T>,
  field3: PropertyCoder<V3, T>
): Coder<T> {
  return addProxyCoder(field1, field2, field3) { inStream ->
    build(field1.decode(inStream), field2.decode(inStream), field3.decode(inStream))
  }
}
