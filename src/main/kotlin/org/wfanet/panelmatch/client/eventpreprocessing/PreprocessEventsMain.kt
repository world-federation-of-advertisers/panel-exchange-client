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
import org.apache.beam.sdk.options.Default
import org.apache.beam.sdk.options.Description
import org.apache.beam.sdk.options.PipelineOptions
import org.apache.beam.sdk.options.PipelineOptionsFactory
import org.apache.beam.sdk.options.Validation
import org.apache.beam.sdk.transforms.DoFn
import org.apache.beam.sdk.transforms.ParDo
import org.apache.beam.sdk.values.KV
import org.wfanet.panelmatch.common.beam.kvOf

private const val EVENT_TABLE = "INSERT TABLE ADDRESS HERE"

interface Options : PipelineOptions {
  @get:Description("Batch Size") var batchSize: Int

  @get:Description("Cryptokey") var cryptokey: String

  @get:Description("Pepper") var pepper: String

  @get:Description("Table to read from, specified as <project_id>:<dataset_id>.<table_id>")
  @get:Default.String(EVENT_TABLE)
  var input: String

  @get:Description(
    "BigQuery table to write to, specified as <project_id>:<dataset_id>.<table_id>. The dataset must already exist."
  )
  @get:Validation.Required
  var outputFile: String
}

fun main(args: Array<String>) {
  val options = PipelineOptionsFactory.fromArgs(*args).withValidation() as Options
  val p = Pipeline.create(options)
  // Build the table schema for the output table.
  val fields =
    arrayListOf<TableFieldSchema>(
      TableFieldSchema().setName("encrypted_id").setType("LONG"),
      TableFieldSchema().setName("encrypted_data").setType("BYTESTRING")
    )
  val schema = TableSchema().setFields(fields)

  // Build the read options proto for the read operation.
  val rowsFromBigQuery =
    p.apply(
      BigQueryIO.readTableRows()
        .from(options.input)
        .withMethod(Method.DIRECT_READ)
        .withSelectedFields(mutableListOf("id", "event"))
    )
  val pairs = rowsFromBigQuery.apply(ParDo.of(PairDoFn))
  val encrypted =
    preprocessEventsInPipeline(
      pairs,
      options.batchSize,
      ByteString.copyFromUtf8(options.pepper),
      ByteString.copyFromUtf8(options.cryptokey)
    )

  val tableRows = encrypted.apply(ParDo.of(TablRowDoFn))
  tableRows.apply(
    BigQueryIO.writeTableRows()
      .to(options.outputFile)
      .withSchema(schema)
      .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
      .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_TRUNCATE)
  )
  p.run().waitUntilFinish()
}

object PairDoFn : DoFn<TableRow, KV<ByteString, ByteString>>() {
  @ProcessElement
  fun process(c: ProcessContext) {
    c.output(
      kvOf(
        ByteString.copyFromUtf8(c.element()["id"] as String),
        ByteString.copyFromUtf8(c.element()["event"] as String)
      )
    )
  }
}

object TablRowDoFn : DoFn<KV<Long, ByteString>, TableRow>() {
  @ProcessElement
  fun process(c: ProcessContext) {
    val tablerow = TableRow().set("encrypted_id", c.element().key)
    tablerow.set("encrypted_data", c.element().value)
    c.output(tablerow)
  }
}
