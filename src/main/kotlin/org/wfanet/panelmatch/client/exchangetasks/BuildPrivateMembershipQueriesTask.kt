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
import org.wfanet.panelmatch.client.privatemembership.CreateQueriesWorkflow
import org.wfanet.panelmatch.client.privatemembership.EncryptedQueryBundle
import org.wfanet.panelmatch.client.privatemembership.PanelistKeyAndJoinKey
import org.wfanet.panelmatch.client.privatemembership.PrivateMembershipCryptor
import org.wfanet.panelmatch.client.privatemembership.QueryIdAndPanelistKey
import org.wfanet.panelmatch.client.storage.VerifiedStorageClient
import org.wfanet.panelmatch.common.ShardedFileName
import org.wfanet.panelmatch.common.beam.map
import org.wfanet.panelmatch.common.toByteString

class BuildPrivateMembershipQueriesTask(
  override val localCertificate: X509Certificate,
  override val uriPrefix: String,
  override val privateKey: PrivateKey,
  private val parameters: CreateQueriesWorkflow.Parameters,
  private val privateMembershipCryptor: PrivateMembershipCryptor,
  private val encryptedQueryBundleFileCount: Int,
  private val queryIdAndPanelistKeyFileCount: Int,
) : ModelProviderApacheBeamTask() {
  override suspend fun execute(
    input: Map<String, VerifiedStorageClient.VerifiedBlob>
  ): Map<String, Flow<ByteString>> {
    val pipeline = Pipeline.create()

    // TODO: previous steps need to output in this format.
    // TODO: need to update all file names to translate labels via step.inputsMap/outputsMap
    val panelistKeyAndJoinKeysManifest = input.getValue("panelists-and-joinkeys")
    val panelistKeyAndJoinKeys =
      readFromManifest(panelistKeyAndJoinKeysManifest, localCertificate).map {
        PanelistKeyAndJoinKey.parseFrom(it)
      }

    val (
      queryIdAndPanelistKeys: PCollection<QueryIdAndPanelistKey>,
      encryptedResponses: PCollection<EncryptedQueryBundle>) =
      CreateQueriesWorkflow(parameters, privateMembershipCryptor)
        .batchCreateQueries(panelistKeyAndJoinKeys, privateMembershipKeys)

    val queryDecryptionKeysFileSpec =
      ShardedFileName("query-decryption-keys", queryIdAndPanelistKeyFileCount)
    queryIdAndPanelistKeys.map { it.toByteString() }.write(queryDecryptionKeysFileSpec)

    val encryptedQueriesFileSpec =
      ShardedFileName("encrypted-queries", encryptedQueryBundleFileCount)
    encryptedResponses.map { it.toByteString() }.write(encryptedQueriesFileSpec)

    pipeline.run()

    return mapOf(
      "query-decryption-keys" to flowOf(queryDecryptionKeysFileSpec.spec.toByteString()),
      "encrypted-queries" to flowOf(encryptedQueriesFileSpec.spec.toByteString())
    )
  }
}