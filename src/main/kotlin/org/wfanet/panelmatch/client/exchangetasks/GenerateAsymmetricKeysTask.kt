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
import org.wfanet.measurement.storage.StorageClient
import org.wfanet.panelmatch.client.logger.addToTaskLog
import org.wfanet.panelmatch.client.logger.loggerFor
import org.wfanet.panelmatch.client.storage.VerifiedStorageClient.VerifiedBlob
import org.wfanet.panelmatch.common.crypto.AsymmetricKeys

private const val PRIVATE_KEY_LABEL = "private-key"
private const val PUBLIC_KEY_LABEL = "public-key"

class GenerateAsymmetricKeysTask(private val generateKeys: () -> AsymmetricKeys) : ExchangeTask {

  override suspend fun execute(input: Map<String, StorageClient.Blob>): Map<String, Flow<ByteString>> {
    logger.addToTaskLog("Executing generate asymmetric keys")

    val key = generateKeys()
    return mapOf(
      PUBLIC_KEY_LABEL to flowOf(key.serializedPublicKey),
      PRIVATE_KEY_LABEL to flowOf(key.serializedPrivateKey)
    )
  }

  companion object {
    private val logger by loggerFor()
  }
}
