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

import com.google.api.services.bigquery.model.TableFieldSchema
import com.google.api.services.bigquery.model.TableRow
import com.google.api.services.bigquery.model.TableSchema
import com.google.protobuf.ByteString
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.TypedRead.Method
import org.apache.beam.sdk.options.Description
import org.apache.beam.sdk.options.PipelineOptionsFactory
import org.apache.beam.sdk.options.Validation
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.wfanet.panelmatch.common.beam.kvOf
import org.wfanet.panelmatch.common.beam.map

interface Options : DataflowPipelineOptions {

  @get:Description("Batch Size") var batchSize: Int

  @get:Description("Cryptokey") var cryptokey: String

  @get:Description("Pepper") var pepper: String

  @get:Description("Table to read from, specified as <project_id>:<dataset_id>.<table_id>")
  @get:Validation.Required
  var bigQueryInputTable: String

  @get:Description(
    "BigQuery table to write to, specified as <project_id>:<dataset_id>.<table_id>. " +
      "The dataset must already exist."
  )
  @get:Validation.Required
  var bigQueryOutputTable: String
}

fun main(args: Array<String>) {
  val options = PipelineOptionsFactory.fromArgs(*args).withValidation().`as`(Options::class.java)
  val p = Pipeline.create(options)

  val unencryptedEvents = readFromBigQuery(options.bigQueryInputTable, p)
  val encryptedEvents =
    preprocessEventsInPipeline(
      unencryptedEvents,
      options.batchSize,
      ByteString.copyFromUtf8(options.pepper),
      ByteString.copyFromUtf8(options.cryptokey)
    )

  writeToBigQuery(encryptedEvents, options.bigQueryOutputTable)

  p.run().waitUntilFinish()
}

fun writeToBigQuery(encryptedEvents: PCollection<KV<Long, ByteString>>, outputTable: String) {
  // Build the table schema for the output table.
  val fields =
    listOf<TableFieldSchema>(
      TableFieldSchema().setName("encrypted_id").setType("LONG"),
      TableFieldSchema().setName("encrypted_data").setType("BYTESTRING")
    )
  val schema = TableSchema().setFields(fields)

  // Convert KV<Long,ByteString> to TableRow
  val encryptedTableRows =
    encryptedEvents.map { TableRow().set("encrypted_id", it.key).set("encrypted_data", it.value) }

  // Write to BigQueryIO
  encryptedTableRows.apply(
    BigQueryIO.writeTableRows()
      .to(outputTable)
      .withSchema(schema)
      .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
      .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_TRUNCATE)
  )
}

fun readFromBigQuery(inputTable: String, p: Pipeline): PCollection<KV<ByteString, ByteString>> {
  // Build the read options proto for the read operation.
  val rowsFromBigQuery =
    p.apply(
      BigQueryIO.readTableRows()
        .from(inputTable)
        .withMethod(Method.DIRECT_READ)
        .withSelectedFields(mutableListOf("id", "data"))
    )
  val unencryptedEvents =
    rowsFromBigQuery.map {
      kvOf(
        ByteString.copyFromUtf8(it["id"] as String),
        ByteString.copyFromUtf8(it["data"] as String)
      )
    }
  return unencryptedEvents
}
