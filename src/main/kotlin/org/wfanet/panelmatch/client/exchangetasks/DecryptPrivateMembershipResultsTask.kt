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
import java.security.PrivateKey
import java.security.cert.X509Certificate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.values.PCollection
import org.wfanet.panelmatch.client.privatemembership.DecryptQueryResultsWorkflow
import org.wfanet.panelmatch.client.privatemembership.DecryptedEventDataSet
import org.wfanet.panelmatch.client.privatemembership.EncryptedQueryResult
import org.wfanet.panelmatch.client.privatemembership.QueryIdAndJoinKey
import org.wfanet.panelmatch.client.privatemembership.QueryResultsDecryptor
import org.wfanet.panelmatch.client.storage.VerifiedStorageClient.VerifiedBlob
import org.wfanet.panelmatch.common.ShardedFileName
import org.wfanet.panelmatch.common.beam.kvOf
import org.wfanet.panelmatch.common.beam.map
import org.wfanet.panelmatch.common.beam.toSingletonView
import org.wfanet.panelmatch.common.compression.CompressorFactory
import org.wfanet.panelmatch.common.toByteString

class DecryptPrivateMembershipResultsTask(
  override val uriPrefix: String,
  override val privateKey: PrivateKey,
  override val localCertificate: X509Certificate,
  private val serializedParameters: ByteString,
  private val queryResultsDecryptor: QueryResultsDecryptor,
  private val compressorFactory: CompressorFactory,
  private val partnerCertificate: X509Certificate,
  private val decryptedEventDataSetFileCount: Int,
) : ModelProviderApacheBeamTask() {
  override suspend fun execute(input: Map<String, VerifiedBlob>): Map<String, Flow<ByteString>> {
    val pipeline = Pipeline.create()

    val encryptedQueryResultsFileSpec = input.getValue("encrypted-query-results")
    val encryptedQueryResults =
      readFromManifest(encryptedQueryResultsFileSpec, partnerCertificate).map {
        EncryptedQueryResult.parseFrom(it)
      }

    val queryToJoinKeyFileSpec = input.getValue("query-to-joinkey-map")
    val queryToJoinKey =
      readFromManifest(queryToJoinKeyFileSpec, localCertificate).map {
        with(QueryIdAndJoinKey.parseFrom(it)) { kvOf(queryId, joinKey) }
      }

    val dictionary =
      readFileAsSingletonPCollection("compression-dictionary", partnerCertificate).toSingletonView()

    val hkdfPepper = input.getValue("hkdf-pepper").toByteString()

    val decryptedEventDataSet: PCollection<DecryptedEventDataSet> =
      DecryptQueryResultsWorkflow(
          serializedParameters,
          queryResultsDecryptor,
          hkdfPepper,
          compressorFactory
        )
        .batchDecryptQueryResults(
          encryptedQueryResults,
          queryToJoinKey,
          dictionary,
          privateMembershipKeys
        )

    val fileSpec = ShardedFileName("decrypted-event-data", decryptedEventDataSetFileCount)
    decryptedEventDataSet.map { it.toByteString() }.write(fileSpec)

    pipeline.run()

    return mapOf("decrypted-event-data" to flowOf(fileSpec.spec.toByteString()))
  }
}