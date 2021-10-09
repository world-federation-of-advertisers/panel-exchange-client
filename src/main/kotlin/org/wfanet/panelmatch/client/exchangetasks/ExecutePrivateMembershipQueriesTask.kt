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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.wfanet.panelmatch.client.privatemembership.DatabaseEntries
import org.wfanet.panelmatch.client.privatemembership.DatabaseEntry
import org.wfanet.panelmatch.client.privatemembership.EncryptedQueryBundle
import org.wfanet.panelmatch.client.privatemembership.EncryptedQueryResults
import org.wfanet.panelmatch.client.privatemembership.EvaluateQueriesParameters
import org.wfanet.panelmatch.client.privatemembership.QueryEvaluator
import org.wfanet.panelmatch.client.privatemembership.ShardId
import org.wfanet.panelmatch.client.privatemembership.evaluateQueries
import org.wfanet.panelmatch.client.storage.StorageFactory
import org.wfanet.panelmatch.client.storage.VerifiedStorageClient.VerifiedBlob
import org.wfanet.panelmatch.common.ShardedFileName
import org.wfanet.panelmatch.common.beam.flatMap
import org.wfanet.panelmatch.common.beam.map
import org.wfanet.panelmatch.common.beam.mapKeys
import org.wfanet.panelmatch.common.beam.mapValues
import org.wfanet.panelmatch.common.beam.toSingletonView
import org.wfanet.panelmatch.common.toByteString

/** Evaluates Private Membership queries. */
class ExecutePrivateMembershipQueriesTask(
  override val storageFactory: StorageFactory,
  private val evaluateQueriesParameters: EvaluateQueriesParameters,
  private val queryEvaluator: QueryEvaluator,
  private val publicKeyBlobKey: String,
  private val outputs: Outputs
) : ApacheBeamTask() {

  data class Outputs(
    val encryptedQueryResultFileName: String,
    val encryptedQueryResultFileCount: Int
  )

  override suspend fun execute(input: Map<String, VerifiedBlob>): Map<String, Flow<ByteString>> {
    val pipeline = Pipeline.create()

    val databaseManifest = input.getValue("event-data")
    val database: PCollection<DatabaseEntry> =
      readFromManifest(databaseManifest).flatMap { DatabaseEntries.parseFrom(it).entriesList }

    val queriesManifest = input.getValue("encrypted-queries")
    val queries = readFromManifest(queriesManifest).map { EncryptedQueryBundle.parseFrom(it) }

    val privateMembershipPublicKey = readSingleBlobAsPCollection(publicKeyBlobKey).toSingletonView()

    val results: PCollection<KV<ShardId, EncryptedQueryResults>> =
      evaluateQueries(
        database,
        queries,
        privateMembershipPublicKey,
        evaluateQueriesParameters,
        queryEvaluator
      )

    val encryptedResultsFileSpec =
      ShardedFileName(outputs.encryptedQueryResultFileName, outputs.encryptedQueryResultFileCount)
    results.mapKeys { it.id }.mapValues { it.toByteString() }.write(encryptedResultsFileSpec)

    pipeline.run()

    return mapOf("encrypted-results" to flowOf(encryptedResultsFileSpec.spec.toByteString()))
  }
}
