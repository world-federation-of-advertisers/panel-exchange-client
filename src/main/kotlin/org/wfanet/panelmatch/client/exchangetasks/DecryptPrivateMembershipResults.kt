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
import org.apache.beam.sdk.values.PCollection
import org.wfanet.panelmatch.client.privatemembership.EncryptedQueryResult
import org.wfanet.panelmatch.client.privatemembership.KeyedDecryptedEventDataSet
import org.wfanet.panelmatch.client.privatemembership.QueryResultsDecryptor
import org.wfanet.panelmatch.client.privatemembership.decryptQueryResults
import org.wfanet.panelmatch.client.privatemembership.encryptedQueryResult
import org.wfanet.panelmatch.client.privatemembership.queryIdAndJoinKeys
import org.wfanet.panelmatch.common.beam.map
import org.wfanet.panelmatch.common.beam.mapWithSideInput
import org.wfanet.panelmatch.common.beam.toSingletonView
import org.wfanet.panelmatch.common.compression.CompressionParameters
import org.wfanet.panelmatch.common.crypto.AsymmetricKeys

suspend fun ApacheBeamContext.decryptPrivateMembershipResults(
  serializedParameters: ByteString,
  queryResultsDecryptor: QueryResultsDecryptor,
) {
  val encryptedQueryResults: PCollection<EncryptedQueryResult> =
    readShardedPCollection("encrypted-query-results", encryptedQueryResult {})

  val queryAndJoinKeys = readShardedPCollection("query-to-join-keys-map", queryIdAndJoinKeys {})

  val compressionParameters =
    readBlobAsPCollection("compression-parameters")
      .map("Parse as CompressionParameters") { CompressionParameters.parseFrom(it) }
      .toSingletonView()

  val hkdfPepper = readBlob("hkdf-pepper")
  val publicKeyView = readBlobAsView("rlwe-serialized-public-key")

  val privateKeysView =
    readBlobAsPCollection("rlwe-serialized-private-key")
      .mapWithSideInput(publicKeyView, "Make Private Membership Keys") { privateKey, publicKey ->
        AsymmetricKeys(serializedPublicKey = publicKey, serializedPrivateKey = privateKey)
      }
      .toSingletonView()

  val keyedDecryptedEventDataSet: PCollection<KeyedDecryptedEventDataSet> =
    decryptQueryResults(
      encryptedQueryResults,
      queryAndJoinKeys,
      compressionParameters,
      privateKeysView,
      serializedParameters,
      queryResultsDecryptor,
      hkdfPepper,
    )

  keyedDecryptedEventDataSet.write("decrypted-event-data")
}