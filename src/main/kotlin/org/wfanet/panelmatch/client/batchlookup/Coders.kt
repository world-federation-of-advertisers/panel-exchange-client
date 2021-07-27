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
import org.apache.beam.sdk.coders.AtomicCoder
import org.apache.beam.sdk.coders.VarLongCoder
import org.apache.beam.sdk.extensions.protobuf.ByteStringCoder
import org.apache.beam.sdk.extensions.protobuf.ProtoCoder

/** Adds coders for types in this package to the receiver's CoderRegistry. */
fun Pipeline.registerBatchLookupCoders() {
  coderRegistry.registerCoderProvider(ProtoCoder.getCoderProvider())

  coderRegistry.registerCoderForClass(
    DatabaseKey::class.java,
    object : AtomicCoder<DatabaseKey>() {
      private val longCoder = VarLongCoder.of()

      override fun encode(value: DatabaseKey, outStream: OutputStream) {
        longCoder.encode(value.id, outStream)
      }

      override fun decode(inStream: InputStream): DatabaseKey {
        return DatabaseKey(longCoder.decode(inStream))
      }

      override fun verifyDeterministic() {}
    }
  )

  coderRegistry.registerCoderForClass(
    Plaintext::class.java,
    object : AtomicCoder<Plaintext>() {
      private val bytesCoder = ByteStringCoder.of()

      override fun encode(value: Plaintext, outStream: OutputStream) {
        bytesCoder.encode(value.data, outStream)
      }

      override fun decode(inStream: InputStream): Plaintext {
        return Plaintext(bytesCoder.decode(inStream))
      }

      override fun verifyDeterministic() {}
    }
  )
}
