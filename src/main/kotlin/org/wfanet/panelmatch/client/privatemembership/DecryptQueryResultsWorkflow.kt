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

import java.io.Serializable
import java.util.BitSet
import com.google.protobuf.ByteString
import kotlin.math.abs
import kotlin.random.Random
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.wfanet.panelmatch.common.beam.groupByKey
import org.wfanet.panelmatch.common.beam.keyBy
import org.wfanet.panelmatch.common.beam.kvOf
import org.wfanet.panelmatch.common.beam.map
import org.wfanet.panelmatch.common.beam.parDo
import org.wfanet.panelmatch.common.beam.values

/**
 * Implements a query decryption engine in Apache Beam that decrypts a query result
 *
 * @param parameters tuning knobs for the workflow
 * @param privateMembershipCryptor implementation of lower-level oblivious query expansion and
 * result decryption
 */
class DecryptQueryResultsWorkflow(
  private val parameters: Parameters,
  private val privateMembershipCryptor: PrivateMembershipCryptor
) : Serializable {

  /**
   * Tuning knobs for the [CreateQueriesWorkflow].
   *
   */
  data class Parameters(
    val obliviousQueryParameters: ObliviousQueryParameters
    ) :
    Serializable

  /** Creates [EncryptQueriesResponse] on [data]. */
  fun batchDecryptQueryResults(
    encryptedQueryResults: PCollection<ByteString>
  ): PCollection<ByteString> {
      return encryptedQueryResults.parDo {
        val decryptQueryResultsRequest = DecryptQueriesRequest.newBuilder()
          .setParameters(parameters.obliviousQueryParameters)
          //TODO set public and private key
          .addAllEncryptedQueryResults(listOf(it))
          .build()
        val decryptedResults = privateMembershipCryptor.decryptQueryResults(decryptQueryResultsRequest)
        yieldAll(decryptedResults.getDecryptedQueryResultsList())
    }
      //TODO remove AES encryption batches
  }
}
