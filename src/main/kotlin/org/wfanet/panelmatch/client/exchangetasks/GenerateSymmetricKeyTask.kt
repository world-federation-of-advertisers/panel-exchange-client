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
import org.wfanet.panelmatch.common.crypto.DeterministicCommutativeCipher

private const val OUTPUT_DATA_LABEL = "symmetric-key"

/** A task for generating an symmetric key for a [DeterministicCommutativeCipher]. */
class GenerateSymmetricKeyTask(private val cipher: DeterministicCommutativeCipher) : ExchangeTask {
  override suspend fun execute(
    input: Map<String, StorageClient.Blob>
  ): Map<String, Flow<ByteString>> {
    return mapOf(OUTPUT_DATA_LABEL to flowOf(cipher.generateKey()))
  }
}
