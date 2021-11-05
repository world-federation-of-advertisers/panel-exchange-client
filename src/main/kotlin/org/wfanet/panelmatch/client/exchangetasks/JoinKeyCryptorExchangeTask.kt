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
import org.wfanet.measurement.common.asBufferedFlow
import org.wfanet.measurement.storage.StorageClient
import org.wfanet.panelmatch.client.joinkeyexchange.JoinKeyAndId
import org.wfanet.panelmatch.client.joinkeyexchange.JoinKeyAndIdCollection
import org.wfanet.panelmatch.client.joinkeyexchange.JoinKeyCryptor
import org.wfanet.panelmatch.client.joinkeyexchange.joinKeyAndIdCollection
import org.wfanet.panelmatch.client.logger.addToTaskLog
import org.wfanet.panelmatch.client.logger.loggerFor
import org.wfanet.panelmatch.common.storage.toByteString

private const val INPUT_CRYPTO_KEY_LABEL = "encryption-key"

class JoinKeyCryptorExchangeTask
internal constructor(
  private val operation: (ByteString, List<JoinKeyAndId>) -> List<JoinKeyAndId>,
  private val inputDataLabel: String,
  private val outputDataLabel: String
) : ExchangeTask {

  override suspend fun execute(
    input: Map<String, StorageClient.Blob>
  ): Map<String, Flow<ByteString>> {
    logger.addToTaskLog("Executing crypto operation")

    // TODO See if it is worth updating this to not collect the inputs entirely at this step.
    //  It should be possible to process batches of them to balance memory usage and execution
    //  efficiency. If the inputs turn out to be small enough this shouldn't be an issue.
    //  Another reason to process them in one entire batch is to minimize the cost of
    //  serialization.
    val cryptoKey = input.getValue(INPUT_CRYPTO_KEY_LABEL).toByteString()
    val serializedInputs = input.getValue(inputDataLabel).toByteString()
    val results =
      operation(cryptoKey, JoinKeyAndIdCollection.parseFrom(serializedInputs).joinKeysAndIdsList)

    val serializedOutput = joinKeyAndIdCollection { joinKeysAndIds += results }.toByteString()
    return mapOf(outputDataLabel to serializedOutput.asBufferedFlow(1024))
  }

  companion object {
    private val logger by loggerFor()

    /** Returns an [ExchangeTask] that removes encryption from data. */
    @JvmStatic
    fun forDecryption(JoinKeyCryptor: JoinKeyCryptor): ExchangeTask {
      return JoinKeyCryptorExchangeTask(
        operation = JoinKeyCryptor::decrypt,
        inputDataLabel = "encrypted-data",
        outputDataLabel = "decrypted-data"
      )
    }

    /** Returns an [ExchangeTask] that adds encryption to plaintext. */
    @JvmStatic
    fun forEncryption(JoinKeyCryptor: JoinKeyCryptor): ExchangeTask {
      return JoinKeyCryptorExchangeTask(
        operation = JoinKeyCryptor::encrypt,
        inputDataLabel = "unencrypted-data",
        outputDataLabel = "encrypted-data"
      )
    }

    /** Returns an [ExchangeTask] that adds another layer of encryption to data. */
    @JvmStatic
    fun forReEncryption(JoinKeyCryptor: JoinKeyCryptor): ExchangeTask {
      return JoinKeyCryptorExchangeTask(
        operation = JoinKeyCryptor::reEncrypt,
        inputDataLabel = "encrypted-data",
        outputDataLabel = "reencrypted-data"
      )
    }
  }
}
