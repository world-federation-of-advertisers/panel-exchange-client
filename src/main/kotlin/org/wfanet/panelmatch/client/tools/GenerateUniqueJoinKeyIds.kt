package org.wfanet.panelmatch.client.tools

import com.google.protobuf.kotlin.toByteStringUtf8
import java.io.File
import java.io.PrintWriter
import java.util.UUID
import org.wfanet.measurement.common.commandLineMain
import org.wfanet.panelmatch.client.exchangetasks.JoinKeyAndIdCollection
import org.wfanet.panelmatch.client.exchangetasks.JoinKeyIdentifier
import org.wfanet.panelmatch.client.exchangetasks.copy
import org.wfanet.panelmatch.client.exchangetasks.joinKeyAndIdCollection
import org.wfanet.panelmatch.client.exchangetasks.joinKeyIdentifier
import picocli.CommandLine.Command
import picocli.CommandLine.Option

@Command(
  name = "generate-unique-join-key-ids",
  description = ["Reads an input JoinKeyAndIdCollection and replaces the IDs with unique ones."],
)
class GenerateUniqueJoinKeyIds : Runnable {

  @Option(
    names = ["--input-join-keys-path"],
    description = ["Path to a file containing a serialized JoinKeyAndIdCollection."],
    required = true,
  )
  private lateinit var inputJoinKeysFile: File

  @Option(
    names = ["--output-join-keys-path"],
    description = ["Path to the file where a corrected JoinKeyAndIdCollection will be written."],
    required = true,
  )
  private lateinit var outputJoinKeysFile: File

  @Option(
    names = ["--output-join-key-id-mapping-path"],
    description = ["Path to the CSV file mapping original JKID to unique JKID."],
    required = true,
  )
  private lateinit var outputJoinKeyIdMappingFile: File

  override fun run() {
    val inputJoinKeys = JoinKeyAndIdCollection.parseFrom(inputJoinKeysFile.readBytes())
    val mappings = mutableListOf<JoinKeyIdMapping>()

    val outputJoinKeys = joinKeyAndIdCollection {
      for (joinKeyAndId in inputJoinKeys.joinKeyAndIdsList) {
        val uniqueId = joinKeyIdentifier { id = UUID.randomUUID().toString().toByteStringUtf8() }
        val mapping = JoinKeyIdMapping(
          originalId = joinKeyAndId.joinKeyIdentifier,
          uniqueId = uniqueId
        )

        mappings += mapping
        joinKeyAndIds += joinKeyAndId.copy { joinKeyIdentifier = uniqueId }
      }
    }

    outputJoinKeysFile.writeBytes(outputJoinKeys.toByteArray())
    PrintWriter(outputJoinKeyIdMappingFile.outputStream()).use { printWriter ->
      printWriter.println("OriginalJoinKeyId,UniqueJoinKeyId")
      for (mapping in mappings) {
        printWriter.println(mapping)
      }
    }
  }
}

private data class JoinKeyIdMapping(
  private val originalId: JoinKeyIdentifier,
  private val uniqueId: JoinKeyIdentifier,
) {
  override fun toString(): String = "${originalId.id.toStringUtf8()},${uniqueId.id.toStringUtf8()}"
}

fun main(args: Array<String>) = commandLineMain(GenerateUniqueJoinKeyIds(), args)
