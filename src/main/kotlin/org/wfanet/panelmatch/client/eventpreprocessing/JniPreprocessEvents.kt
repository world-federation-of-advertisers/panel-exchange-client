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

import java.nio.file.Files
import java.util.logging.Level
import org.wfanet.panelmatch.client.PreprocessEventsRequest
import org.wfanet.panelmatch.client.PreprocessEventsResponse
import org.wfanet.panelmatch.client.logger.loggerFor
import org.wfanet.panelmatch.common.getRuntimePath
import org.wfanet.panelmatch.common.loadLibrary
import org.wfanet.panelmatch.common.pathOf
import org.wfanet.panelmatch.common.wrapJniException

/** A [PreprocessEvents] implementation using the JNI [PreprocessEvents]. */
class JniPreprocessEvents : PreprocessEvents {

  override fun preprocess(request: PreprocessEventsRequest): PreprocessEventsResponse {
    return wrapJniException {
      PreprocessEventsResponse.parseFrom(
        EventPreprocessing.preprocessEventsWrapper(request.toByteArray())
      )
    }
  }

  companion object {
    private val logger by loggerFor()

    init {
      val directory =
        pathOf("panel_exchange_client/src/main/swig/wfanet/panelmatch/client/eventpreprocessing")
      try {
        loadLibrary(name = "preprocess_events", directoryPath = directory)
      } catch (e: Exception) {
        /*
         * This is a massive hack for Google Cloud Dataflow.
         *
         * TODO: switch to using `getResource` here from a jar. (Github Issue #77)
         */
        logger.log(Level.WARNING, "loadLibrary exception", e)
        val dataflowDir = pathOf("/var/opt/google/dataflow").toFile()
        if (dataflowDir.exists() && dataflowDir.isDirectory) {
          val libraryFile =
            dataflowDir.listFiles()!!.single {
              it.name.startsWith("libpreprocess_events-") && it.extension == "so"
            }

          val tmpDirPath = checkNotNull(getRuntimePath(directory))
          if (!tmpDirPath.toFile().exists()) {
            Files.createDirectories(tmpDirPath)
          }
          val destinationPath = tmpDirPath.resolve("libpreprocess_events.so")
          if (!destinationPath.toFile().exists()) {
            libraryFile.copyTo(destinationPath.toFile())
          }
          check(destinationPath.toFile().setExecutable(true))
          loadLibrary(name = "preprocess_events", directoryPath = directory)
        } else {
          throw e
        }
      }
    }
  }
}
