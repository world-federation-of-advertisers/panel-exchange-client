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
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.TypedRead.Method
import org.apache.beam.sdk.options.Description
import org.apache.beam.sdk.options.PipelineOptions
import org.apache.beam.sdk.options.PipelineOptionsFactory
import org.apache.beam.sdk.options.Validation
import org.wfanet.panelmatch.common.beam.kvOf
import org.wfanet.panelmatch.common.beam.map

private const val EVENT_TABLE = "INSERT TABLE ADDRESS HERE"

interface Options : PipelineOptions {
  @get:Description("Batch Size") var batchSize: Int

  @get:Description("Cryptokey") var cryptokey: String

  @get:Description("Pepper") var pepper: String

  @get:Validation.Required
  @get:Description("Table to read from, specified as <project_id>:<dataset_id>.<table_id>")
  var bigQueryInputTable: String

  @get:Description(
    "BigQuery table to write to, specified as <project_id>:<dataset_id>.<table_id>. The dataset must already exist."
  )
  @get:Validation.Required
  var bigQueryOutputTable: String
}

fun main(args: Array<String>) {
  val options = PipelineOptionsFactory.fromArgs(*args).withValidation() as Options
  val p = Pipeline.create(options)
  // Build the table schema for the output table.
  val fields =
    listOf<TableFieldSchema>(
      TableFieldSchema().setName("encrypted_id").setType("LONG"),
      TableFieldSchema().setName("encrypted_data").setType("BYTESTRING")
    )
  val schema = TableSchema().setFields(fields)

  // Build the read options proto for the read operation.
  val rowsFromBigQuery =
    p.apply(
      BigQueryIO.readTableRows()
        .from(options.bigQueryInputTable)
        .withMethod(Method.DIRECT_READ)
        .withSelectedFields(mutableListOf("id", "data"))
    )
  val pairs =
    rowsFromBigQuery.map {
      kvOf(
        ByteString.copyFromUtf8(it["id"] as String),
        ByteString.copyFromUtf8(it["data"] as String)
      )
    }
  val encrypted =
    preprocessEventsInPipeline(
      pairs,
      options.batchSize,
      ByteString.copyFromUtf8(options.pepper),
      ByteString.copyFromUtf8(options.cryptokey)
    )

  val tableRows =
    encrypted.map {
      val tablerow = TableRow().set("encrypted_id", it.key)
      tablerow.set("encrypted_data", it.value)
    }
  tableRows.apply(
    BigQueryIO.writeTableRows()
      .to(options.bigQueryOutputTable)
      .withSchema(schema)
      .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
      .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_TRUNCATE)
  )
  p.run().waitUntilFinish()
}
