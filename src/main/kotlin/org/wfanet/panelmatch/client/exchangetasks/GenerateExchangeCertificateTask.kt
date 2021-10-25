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
import org.wfanet.measurement.storage.StorageClient
import org.wfanet.panelmatch.common.certificates.CertificateManager

/**
 * Generate Exchange Certificate uses the owner's root cert to generate a new key/certificate pair
 * for the exchange and registers it with the Kingdom via the createCertificate API. The actual API
 * interactions are all handled by certificateManager. The resource ID generated by the response of
 * createCertificate is stored in the output of execute
 */
class GenerateExchangeCertificateTask(private val certificateManager: CertificateManager) :
  ExchangeTask {

  final override suspend fun execute(
    input: Map<String, StorageClient.Blob>
  ): Map<String, Flow<ByteString>> {
    require(input.isEmpty())
    return execute()
  }

  suspend fun execute(): Map<String, Flow<ByteString>> {
    TODO("Not yet implemented")
  }
}
