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

import java.nio.file.Paths
import org.apache.beam.runners.core.construction.resources.PipelineResources
import org.apache.beam.sdk.options.FileStagingOptions
import org.apache.beam.sdk.options.PipelineOptions
import org.wfanet.panelmatch.common.getRuntimePath

/**
 * Sets the `filesToStage` for the pipeline.
 *
 * This is required because we need to add extra files beyond the Java JARs.
 */
fun setFilesToStage(options: FileStagingOptions) {
  options.filesToStage = getDefaultFilesToStage(options) + extraFilesToStage
}

private fun getDefaultFilesToStage(options: PipelineOptions): List<String> {
  val classLoader: ClassLoader = (object : Any() {})::class.java.classLoader
  return PipelineResources.detectClassPathResourcesToStage(classLoader, options)
}

private val extraFilesToStage: List<String> by lazy {
  val swigPath =
    Paths.get("panel_exchange_client/src/main/swig/wfanet/panelmatch/client/eventpreprocessing")
  val relativePath = swigPath.resolve(System.mapLibraryName("preprocess_events"))
  val runtimePath = requireNotNull(getRuntimePath(relativePath))
  listOf(runtimePath.toAbsolutePath().toString())
}
