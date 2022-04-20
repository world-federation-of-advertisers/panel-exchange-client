package org.wfanet.panelmatch.client.tools

import com.google.protobuf.Message
import com.google.protobuf.Parser
import java.io.File
import java.io.PrintWriter
import org.wfanet.measurement.common.commandLineMain
import org.wfanet.measurement.common.toJson
import org.wfanet.panelmatch.client.eventpreprocessing.CombinedEvents
import org.wfanet.panelmatch.client.privatemembership.KeyedDecryptedEventDataSet
import org.wfanet.panelmatch.client.tools.DataProviderEventSetKt.entry
import org.wfanet.panelmatch.common.ShardedFileName
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import wfa_virtual_people.Event.DataProviderEvent

@Command(
  name = "parse-decrypted-event-data",
  description = ["Parses the decrypted event data produced by an exchange."]
)
class ParseDecryptedEventData : Runnable {

  @Option(
    names = ["--manifest-path"],
    description = ["Path to the manifest file of the decrypted event data set."],
    required = true,
  )
  private lateinit var manifestFile: File

  @Option(
    names = ["--output-path"],
    description = ["Path to output the parsed JSON result."],
    required = true,
  )
  private lateinit var outputFile: File

  override fun run() {
    val fileSpec = manifestFile.readText().trim()
    val shardedFileName = ShardedFileName(fileSpec)
    val parsedShards = shardedFileName.parseAllShards(KeyedDecryptedEventDataSet.parser())
    val dataProviderEventSet = dataProviderEventSet {
      entries += parsedShards.map { it.toDataProviderEventSetEntry() }.toList()
    }

    outputFile.outputStream().use { outputStream ->
      PrintWriter(outputStream).use { it.write(dataProviderEventSet.toJson()) }
    }
  }

  private fun KeyedDecryptedEventDataSet.toDataProviderEventSetEntry(): DataProviderEventSet.Entry {
    return entry {
      joinKeyAndId = plaintextJoinKeyAndId
      events +=
        decryptedEventDataList.flatMap { plaintext ->
          val combinedEvents = CombinedEvents.parseFrom(plaintext.payload)
          combinedEvents.serializedEventsList.map { DataProviderEvent.parseFrom(it) }
        }
    }
  }

  private fun <T : Message> ShardedFileName.parseAllShards(parser: Parser<T>): Sequence<T> {
    return sequence {
      fileNames.forEach { fileName ->
        val shardFile = manifestFile.parentFile.resolve(fileName)
        shardFile.inputStream().use {
          while (true) {
            val message: T = parser.parseDelimitedFrom(it) ?: break
            yield(message)
          }
        }
      }
    }
  }
}

fun main(args: Array<String>) = commandLineMain(ParseDecryptedEventData(), args)
