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

package org.wfanet.panelmatch.client.exchangetasks

import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.toByteStringUtf8
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.values.PCollection
import org.wfanet.measurement.storage.StorageClient
import org.wfanet.panelmatch.client.eventpreprocessing.DeterministicCommutativeCipherKeyProvider
import org.wfanet.panelmatch.client.eventpreprocessing.HkdfPepperProvider
import org.wfanet.panelmatch.client.eventpreprocessing.IdentifierHashPepperProvider
import org.wfanet.panelmatch.client.eventpreprocessing.PreprocessEvents
import org.wfanet.panelmatch.client.eventpreprocessing.unprocessedEvent
import org.wfanet.panelmatch.client.privatemembership.DatabaseEntry
import org.wfanet.panelmatch.client.storage.StorageFactory
import org.wfanet.panelmatch.common.ShardedFileName
import org.wfanet.panelmatch.common.beam.map
import org.wfanet.panelmatch.common.beam.toSingletonView
import org.wfanet.panelmatch.common.compression.CompressionParameters
import org.wfanet.panelmatch.common.storage.toByteString
import org.wfanet.panelmatch.common.storage.toStringUtf8

class PreprocessEventsTask(
  override val storageFactory: StorageFactory,
  private val deterministicCommutativeCipherKeyProvider:
    (ByteString) -> DeterministicCommutativeCipherKeyProvider,
  private val hkdfPepperProvider: (ByteString) -> HkdfPepperProvider,
  private val identifierPepperProvider: (ByteString) -> IdentifierHashPepperProvider,
  private val maxByteSize: Long,
  private val outputs: Outputs
) : ApacheBeamTask() {

  data class Outputs(val preprocessedEventsFileName: String, val preprocessedEventsFileCount: Int)

  override suspend fun execute(
    input: Map<String, StorageClient.Blob>
  ): Map<String, Flow<ByteString>> {
    val pipeline = Pipeline.create()

    val unprocessedEventManifest = input.getValue("unprocessed-event-data")
    val unprocessedEventData = readFromManifest(unprocessedEventManifest, unprocessedEvent {})

    val hkdfPepper = input.getValue("hkdf-pepper").toByteString()
    val identifierPepper = input.getValue("identifier-pepper").toByteString()
    val encryptionKey = input.getValue("encryption-key").toByteString()

    val compressionParameters =
      readSingleBlobAsPCollection(input.getValue("compression-parameters").toStringUtf8())
        .map("Parse as CompressionParameters") { CompressionParameters.parseFrom(it) }
        .toSingletonView()

    val preprocessedEvents: PCollection<DatabaseEntry> =
      unprocessedEventData.apply(
        PreprocessEvents(
          maxByteSize,
          identifierPepperProvider(identifierPepper),
          hkdfPepperProvider(hkdfPepper),
          deterministicCommutativeCipherKeyProvider(encryptionKey),
          compressionParameters
        )
      )

    val preprocessedEventsFileSpec =
      ShardedFileName(outputs.preprocessedEventsFileName, outputs.preprocessedEventsFileCount)
    preprocessedEvents.write(preprocessedEventsFileSpec)

    pipeline.run()

    return mapOf(
      "preprocessed-event-data" to flowOf(preprocessedEventsFileSpec.spec.toByteStringUtf8())
    )
  }
}
