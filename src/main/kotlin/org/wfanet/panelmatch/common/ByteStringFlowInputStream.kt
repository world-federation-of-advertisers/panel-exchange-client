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

package org.wfanet.panelmatch.common

import com.google.protobuf.ByteString
import java.io.InputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.runBlocking

class ByteStringFlowInputStream
private constructor(
  private val channel: ReceiveChannel<ByteString>,
  iterator: Iterator<InputStream>
) : ChainedInputStream(iterator) {
  override fun close() {
    channel.cancel()
  }

  companion object {
    fun of(flow: Flow<ByteString>, scope: CoroutineScope): ByteStringFlowInputStream {
      @OptIn(FlowPreview::class) val channel = flow.produceIn(scope)
      val iterator = iterator {
        while (true) {
          try {
            val byteString = runBlocking(scope.coroutineContext) { channel.receive() }
            yield(byteString.newInput())
          } catch (_: ClosedReceiveChannelException) {
            break
          }
        }
      }
      return ByteStringFlowInputStream(channel, iterator)
    }
  }
}
