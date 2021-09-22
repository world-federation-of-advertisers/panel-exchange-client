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

package org.wfanet.panelmatch.client.privatemembership

import com.google.protobuf.ByteString
import java.io.Serializable
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.wfanet.panelmatch.client.eventpostprocessing.uncompressEvents
import org.wfanet.panelmatch.common.beam.join
import org.wfanet.panelmatch.common.beam.keyBy
import org.wfanet.panelmatch.common.beam.kvOf
import org.wfanet.panelmatch.common.beam.parDo
import org.wfanet.panelmatch.common.compression.CompressorFactory

/**
 * Implements a query result decryption engine in Apache Beam that decrypts a query result
 *
 * @param parameters tuning knobs for the workflow
 * @param queryResultsDecryptor implementation of lower-level query result decryption and encrypted
 * event decryption
 * @param hkdfPepper used with joinkey to generate AES key
 * @param getCompressor used to data compressor
 */
class DecryptQueryResultsWorkflow(
  private val parameters: Parameters,
  private val queryResultsDecryptor: QueryResultsDecryptor,
  private val hkdfPepper: ByteString,
  private val compressorFactory: CompressorFactory,
) : Serializable {

  /** Tuning knobs for the [DecryptQueriesWorkflow]. */
  data class Parameters(
    val serializedParameters: ByteString,
    val serializedPublicKey: ByteString,
    val serializedPrivateKey: ByteString
  ) : Serializable

  /**
   * Decrypts [EncryptedQueryResult] into [DecryptedEventData]. Currently assumes that each
   * [QueryId] is unique across each [PCollection] and that each [QueryId] uniquely maps to a single
   * [JoinKey]. Uses the dictionary to decompress the data.
   */
  fun batchDecryptQueryResults(
    encryptedQueryResults: PCollection<EncryptedQueryResult>,
    queryIdToJoinKey: PCollection<KV<QueryId, JoinKey>>,
    dictionary: PCollection<ByteString>,
  ): PCollection<DecryptedEventData> {
    val decryptedQueryResults =
      encryptedQueryResults
        .keyBy<EncryptedQueryResult, QueryId>("Key by Query Id") { requireNotNull(it.queryId) }
        .join<QueryId, EncryptedQueryResult, JoinKey, KV<JoinKey, EncryptedQueryResult>>(
          queryIdToJoinKey
        ) {
          _: QueryId,
          encryptedQueryResultsList: Iterable<EncryptedQueryResult>,
          joinKeys: Iterable<JoinKey> ->
          yield(kvOf(joinKeys.single(), encryptedQueryResultsList.single()))
        }
        .parDo<KV<JoinKey, EncryptedQueryResult>, DecryptedEventData>(
          name = "Decrypt encrypted results"
        ) {
          val request = decryptQueryResultsRequest {
            singleBlindedJoinkey = it.key
            this.encryptedQueryResults += it.value
            serializedParameters = parameters.serializedParameters
            serializedPublicKey = parameters.serializedPublicKey
            serializedPrivateKey = parameters.serializedPrivateKey
            this.hkdfPepper = hkdfPepper
          }
          val decryptedResults = queryResultsDecryptor.decryptQueryResults(request)
          yieldAll(decryptedResults.decryptedEventDataList)
        }
    return uncompressEvents(decryptedQueryResults, dictionary, compressorFactory)
  }
}
