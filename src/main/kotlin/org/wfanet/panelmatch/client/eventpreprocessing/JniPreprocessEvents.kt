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

import java.lang.RuntimeException
import java.nio.file.Paths
import org.wfanet.panelmatch.common.loadLibrary
import wfanet.panelmatch.client.PreprocessEventsRequest
import wfanet.panelmatch.client.PreprocessEventsResponse

/** A [PreprocessEvents] implementation using the JNI [PreprocessEvents]. */
class JniPreprocessEvents : PreprocessEvents {
  /** Indicates something went wrong in C++. */
  class JniException(cause: Throwable) : RuntimeException(cause)

  private fun <T> wrapJniException(block: () -> T): T {
    return try {
      block()
    } catch (e: RuntimeException) {
      throw JniException(e)
    }
  }

  override fun PreprocessEvents(request: PreprocessEventsRequest): PreprocessEventsResponse {
    return PreprocessEventsResponse.parseFrom((PreprocessEvents(request)).toByteString())
  }

  companion object {
    init {
      loadLibrary(
        name = "preprocess_events",
        directoryPath =
          Paths.get(
            "panel_exchange_client/src/main/swig/wfanet/panelmatch/client/eventpreprocessing"
          )
      )
    }
  }
}
