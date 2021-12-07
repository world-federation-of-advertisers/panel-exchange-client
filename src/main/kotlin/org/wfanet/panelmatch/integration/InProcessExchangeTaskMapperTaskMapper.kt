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

package org.wfanet.panelmatch.integration

import org.apache.beam.sdk.options.PipelineOptions
import org.wfanet.measurement.common.throttler.Throttler
import org.wfanet.panelmatch.client.exchangetasks.ApacheBeamTasks
import org.wfanet.panelmatch.client.exchangetasks.ExchangeTaskMapper
import org.wfanet.panelmatch.client.exchangetasks.GenerateKeysTasksImpl
import org.wfanet.panelmatch.client.exchangetasks.JniCommutativeEncryptionTasks
import org.wfanet.panelmatch.client.exchangetasks.PrivateStorageTasksImpl
import org.wfanet.panelmatch.client.exchangetasks.SharedStorageTasksImpl
import org.wfanet.panelmatch.client.exchangetasks.ValidationTasksImpl
import org.wfanet.panelmatch.client.privatemembership.JniPrivateMembershipCryptor
import org.wfanet.panelmatch.client.privatemembership.JniQueryEvaluator
import org.wfanet.panelmatch.client.privatemembership.JniQueryResultsDecryptor
import org.wfanet.panelmatch.client.storage.PrivateStorageSelector
import org.wfanet.panelmatch.client.storage.SharedStorageSelector
import org.wfanet.panelmatch.common.certificates.CertificateManager
import org.wfanet.panelmatch.common.crypto.JniDeterministicCommutativeCipher

fun makeInProcessExchangeTaskMapper(
  privateStorageSelector: PrivateStorageSelector,
  sharedStorageSelector: SharedStorageSelector,
  certificateManager: CertificateManager,
  inputTaskThrottler: Throttler,
  pipelineOptions: PipelineOptions
): ExchangeTaskMapper {
  return ExchangeTaskMapper(
    validationTasks = ValidationTasksImpl(),
    commutativeEncryptionTasks = JniCommutativeEncryptionTasks(JniDeterministicCommutativeCipher()),
    mapReduceTasks =
      ApacheBeamTasks(
        getPrivateMembershipCryptor = ::JniPrivateMembershipCryptor,
        getQueryResultsEvaluator = ::JniQueryEvaluator,
        queryResultsDecryptor = JniQueryResultsDecryptor(),
        privateStorageSelector = privateStorageSelector,
        pipelineOptions = pipelineOptions
      ),
    generateKeysTasks =
      GenerateKeysTasksImpl(
        deterministicCommutativeCryptor = JniDeterministicCommutativeCipher(),
        getPrivateMembershipCryptor = ::JniPrivateMembershipCryptor,
        certificateManager = certificateManager
      ),
    privateStorageTasks = PrivateStorageTasksImpl(privateStorageSelector, inputTaskThrottler),
    sharedStorageTasks = SharedStorageTasksImpl(privateStorageSelector, sharedStorageSelector)
  )
}