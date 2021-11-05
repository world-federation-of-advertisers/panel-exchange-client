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
import org.apache.beam.sdk.values.PCollectionView
import org.wfanet.measurement.storage.StorageClient
import org.wfanet.panelmatch.client.joinkeyexchange.joinKeyAndId
import org.wfanet.panelmatch.client.privatemembership.EncryptedQueryResult
import org.wfanet.panelmatch.client.privatemembership.KeyedDecryptedEventDataSet
import org.wfanet.panelmatch.client.privatemembership.QueryResultsDecryptor
import org.wfanet.panelmatch.client.privatemembership.decryptQueryResults
import org.wfanet.panelmatch.client.privatemembership.encryptedQueryResult
import org.wfanet.panelmatch.client.privatemembership.queryIdAndId
import org.wfanet.panelmatch.client.storage.StorageFactory
import org.wfanet.panelmatch.common.ShardedFileName
import org.wfanet.panelmatch.common.beam.map
import org.wfanet.panelmatch.common.beam.mapWithSideInput
import org.wfanet.panelmatch.common.beam.toSingletonView
import org.wfanet.panelmatch.common.compression.CompressionParameters
import org.wfanet.panelmatch.common.crypto.AsymmetricKeys
import org.wfanet.panelmatch.common.storage.toByteString
import org.wfanet.panelmatch.common.storage.toStringUtf8

class DecryptPrivateMembershipResultsTask(
  override val storageFactory: StorageFactory,
  private val serializedParameters: ByteString,
  private val queryResultsDecryptor: QueryResultsDecryptor,
  private val outputs: Outputs
) : ApacheBeamTask() {

  data class Outputs(
    val keyedDecryptedEventDataSetFileName: String,
    val keyedDecryptedEventDataSetFileCount: Int
  )

  override suspend fun execute(
    input: Map<String, StorageClient.Blob>
  ): Map<String, Flow<ByteString>> {
    val pipeline = Pipeline.create()

    val encryptedQueryResultsFileSpec = input.getValue("encrypted-query-results")
    val encryptedQueryResults: PCollection<EncryptedQueryResult> =
      readFromManifest(encryptedQueryResultsFileSpec, encryptedQueryResult {})

    val queryIdAndIdsFileSpec = input.getValue("query-to-join-keys-map")
    val queryIdAndIds = readFromManifest(queryIdAndIdsFileSpec, queryIdAndId {})

    val plaintextJoinKeyAndIdsFileSpec = input.getValue("plaintext-join-keys-to-id-map")
    val plaintextJoinKeyAndIds = readFromManifest(plaintextJoinKeyAndIdsFileSpec, joinKeyAndId {})

    val decryptedJoinKeyAndIdsFileSpec = input.getValue("decrypted-join-keys-to-id-map")
    val decryptedJoinKeyAndIds = readFromManifest(decryptedJoinKeyAndIdsFileSpec, joinKeyAndId {})

    val compressionParameters =
      readSingleBlobAsPCollection(input.getValue("compression-parameters").toStringUtf8())
        .map("Parse as CompressionParameters") { CompressionParameters.parseFrom(it) }
        .toSingletonView()

    val hkdfPepper = input.getValue("hkdf-pepper").toByteString()

    val privateKeys =
      readSingleBlobAsPCollection(input.getValue("rlwe-serialized-private-key").toStringUtf8())
    val publicKeyView =
      readSingleBlobAsPCollection(input.getValue("rlwe-serialized-public-key").toStringUtf8())
        .toSingletonView()
    val privateMembershipKeys: PCollectionView<AsymmetricKeys> =
      privateKeys
        .mapWithSideInput(publicKeyView, "Make AsymmetricKeys") { privateKey, publicKey ->
          AsymmetricKeys(serializedPublicKey = publicKey, serializedPrivateKey = privateKey)
        }
        .toSingletonView()

    val keyedDecryptedEventDataSet: PCollection<KeyedDecryptedEventDataSet> =
      decryptQueryResults(
        encryptedQueryResults,
        plaintextJoinKeyAndIds,
        decryptedJoinKeyAndIds,
        queryIdAndIds,
        compressionParameters,
        privateMembershipKeys,
        serializedParameters,
        queryResultsDecryptor,
        hkdfPepper,
      )

    val keyedDecryptedEventDataSetFileSpec =
      ShardedFileName(
        outputs.keyedDecryptedEventDataSetFileName,
        outputs.keyedDecryptedEventDataSetFileCount
      )
    keyedDecryptedEventDataSet.write(keyedDecryptedEventDataSetFileSpec)

    pipeline.run()

    return mapOf(
      "decrypted-event-data" to flowOf(keyedDecryptedEventDataSetFileSpec.spec.toByteStringUtf8())
    )
  }
}
