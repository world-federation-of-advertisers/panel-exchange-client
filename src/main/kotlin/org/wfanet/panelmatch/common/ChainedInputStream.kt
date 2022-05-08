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

import java.io.ByteArrayInputStream
import java.io.InputStream

open class ChainedInputStream(private val inputStreams: Iterator<InputStream>) : InputStream() {
  private var currentInputStream: InputStream = ByteArrayInputStream(ByteArray(0))

  final override fun read(): Int {
    val result = currentInputStream.read()
    if (result >= 0) return result
    if (advance()) return read()
    return -1
  }

  final override fun read(b: ByteArray, off: Int, len: Int): Int {
    if (len == 0) return 0

    var bytesRead = 0

    while (bytesRead < len) {
      val readResult = currentInputStream.read(b, off + bytesRead, len - bytesRead)
      if (readResult == -1) {
        if (advance()) continue else if (bytesRead == 0) return -1 else return bytesRead
      }
      bytesRead += readResult
    }

    return bytesRead
  }

  private fun advance(): Boolean {
    if (!inputStreams.hasNext()) return false
    currentInputStream = inputStreams.next()
    return true
  }
}
