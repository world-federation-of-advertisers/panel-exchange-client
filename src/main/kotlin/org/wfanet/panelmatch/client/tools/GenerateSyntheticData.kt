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

package org.wfanet.panelmatch.client.tools

import com.google.protobuf.kotlin.toByteStringUtf8
import java.io.File
import kotlin.random.Random
import kotlin.math.floor
import org.wfanet.panelmatch.client.eventpreprocessing.UnprocessedEvent
import wfa_virtual_people.dataProviderEvent
import wfa_virtual_people.logEvent
import wfa_virtual_people.labelerInput
import org.wfanet.panelmatch.client.eventpreprocessing.unprocessedEvent
import org.wfanet.panelmatch.client.exchangetasks.joinKeyAndIdCollection
import org.wfanet.panelmatch.client.exchangetasks.joinKeyAndId
import org.wfanet.panelmatch.client.exchangetasks.JoinKeyAndId
import org.wfanet.panelmatch.client.exchangetasks.joinKeyIdentifier
import org.wfanet.panelmatch.client.exchangetasks.joinKey
import org.wfanet.panelmatch.common.compression.compressionParameters
import org.wfanet.panelmatch.common.compression.CompressionParametersKt.noCompression
import org.wfanet.panelmatch.common.compression.CompressionParametersKt.brotliCompressionParameters
import org.wfanet.panelmatch.common.toDelimitedByteString
import picocli.CommandLine.Option
import picocli.CommandLine.Command

@kotlin.io.path.ExperimentalPathApi
@Command(name = "edp-event-data", description = ["Generates synthetic data for Panel Match."])
private class GenerateSyntheticData : Runnable {
  @Option(
    names = ["--number_of_events"],
    description = ["Number of UnprocessedEvent protos to generate."],
    required = true,
  )
  private lateinit var numberOfEvents: String

  @Option(
    names = ["--unprocessed_events_file_path"],
    description = ["Path to the file where UnprocessedEvent protos will be written."],
    required = false,
    defaultValue = "edp-unprocessed-events"
  )
  private lateinit var unprocessedEventsFile: File

  @Option(
    names = ["--join_keys_file_path"],
    description = ["Path to the file where JoinKeyAndIdCollection proto will be written."],
    required = false,
    defaultValue = "edp-join-keys"
  )
  private lateinit var joinKeysFile: File

  @Option(
    names = ["--join_key_sample_rate"],
    description = ["The sample rate [0, 1] used for selecting an UnprocessedEvent proto."],
    required = true,
    defaultValue = "0.0005"
  )
  private lateinit var sampleRate: String

  @Option(
    names = ["--brotli_dictionary_path"],
    description = ["Path to a file containing a Brotli dictionary."],
    required = false,
  )
  private lateinit var brotliInputFile: File

  @Option(
    names = ["--output_file_path"],
    description = ["Path to the file where the CompressionParameters proto will be written."],
    required = false
  )
  private lateinit var brotliOutputFile: File

  @Option(
    names = ["--join_key_input_path"],
    description = ["Path to the file where the previous day's join keys are stored."],
    required = true,
  )
  private lateinit var joinKeyInputFile: File

  @Option(
    names = ["--join_key_output_path"],
    description = ["Path to the file where previous day's join keys will be copied to."],
    required = false
  )
  private lateinit var joinKeyOutputFile: File

  /** Creates a JoinKeyAndId proto from the given UnprocessedEvent proto. */
  fun UnprocessedEvent.toJoinKeyAndId(): JoinKeyAndId {
    return joinKeyAndId {
      this.joinKey = joinKey { key = id }
      this.joinKeyIdentifier = joinKeyIdentifier {
        this.id = "${id}-join-key-id".toByteStringUtf8()
      }
    }
  }

  @kotlin.io.path.ExperimentalPathApi
  override fun run() {
    val joinKeyAndIdProtos = mutableListOf<JoinKeyAndId>()
    val events = sequence {
      (1..numberOfEvents.toInt()).forEach {
        val event = generateSyntheticData(it)

        if (Random.nextDouble(1.0) < sampleRate.toDouble()) {
          joinKeyAndIdProtos.add(event.toJoinKeyAndId())
        }

        yield(event)
      }
    }

    unprocessedEventsFile.outputStream().use { outputStream ->
      events.forEach { outputStream.write(it.toDelimitedByteString().toByteArray()) }
    }

    joinKeysFile.outputStream().use { outputStream ->
      outputStream.write(joinKeyAndIdCollection { joinKeyAndIds += joinKeyAndIdProtos }.toByteArray())
    }

    writeCompressionParameters(brotliInputFile, brotliOutputFile)

    joinKeyInputFile.copyTo(joinKeyOutputFile)
  }
}

private fun generateSyntheticData(id: Int): UnprocessedEvent {
  val rawDataProviderEvent = dataProviderEvent {
    this.logEvent = logEvent {
      this.labelerInput = labelerInput {
        this.timestampUsec = floor(Math.random()).toLong()
      }
    }
  }
  return unprocessedEvent {
    this.id = id.toString().toByteStringUtf8()
    this.data = rawDataProviderEvent.toByteString()
  }
}

private fun writeCompressionParameters(brotliFile: File, outputFile: File) {
  if (outputFile.name.isEmpty()) return

  val params = when (brotliFile.name.isEmpty()) {
    true -> compressionParameters { this.uncompressed = noCompression {} }
    false -> compressionParameters {
      this.brotli = brotliCompressionParameters {
        this.dictionary = brotliFile.readBytes().toString().toByteStringUtf8()
      }
    }
  }

  outputFile.outputStream().use { outputStream ->
    outputStream.write(params.toDelimitedByteString().toByteArray())
  }
}

