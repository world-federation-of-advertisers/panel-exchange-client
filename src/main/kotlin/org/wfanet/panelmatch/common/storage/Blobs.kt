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

package org.wfanet.panelmatch.common.storage

import com.google.common.collect.Iterators
import com.google.protobuf.ByteString
import java.io.InputStream
import java.io.SequenceInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.runBlocking
import org.wfanet.measurement.common.flatten
import org.wfanet.measurement.storage.StorageClient.Blob

/** Reads the blob into a single [ByteString]. */
suspend fun Blob.toByteString(): ByteString = read().flatten()

/** Reads the blob bytes as a UTF8 [String]. */
suspend fun Blob.toStringUtf8(): String = toByteString().toStringUtf8()

/** Returns an [InputStream] that reads the blob's bytes in [scope]. */
suspend fun Blob.toInputStream(scope: CoroutineScope): InputStream {
  @OptIn(FlowPreview::class) val channel: ReceiveChannel<ByteString> = read().produceIn(scope)
  val iterator: Iterator<InputStream> = iterator {
    while (true) {
      try {
        val byteString = runBlocking(scope.coroutineContext) { channel.receive() }
        yield(byteString.newInput())
      } catch (_: ClosedReceiveChannelException) {
        break
      }
    }
  }
  return SequenceInputStream(Iterators.asEnumeration(iterator))
}
