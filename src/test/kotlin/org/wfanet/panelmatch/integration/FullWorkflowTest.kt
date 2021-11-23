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

import com.google.common.truth.Truth
import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.toByteStringUtf8
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.panelmatch.client.common.joinKeyAndIdOf
import org.wfanet.panelmatch.client.exchangetasks.JoinKeyAndIdCollection
import org.wfanet.panelmatch.client.exchangetasks.joinKeyAndIdCollection

private val PLAINTEXT_JOIN_KEYS = joinKeyAndIdCollection {
  joinKeysAndIds +=
    joinKeyAndIdOf("join-key-1".toByteStringUtf8(), "join-key-id-1".toByteStringUtf8())
  joinKeysAndIds +=
    joinKeyAndIdOf("join-key-2".toByteStringUtf8(), "join-key-id-2".toByteStringUtf8())
}

private val EDP_COMMUTATIVE_DETERMINISTIC_KEY = "some-key".toByteStringUtf8()
private val EDP_IDENTIFIER_HASH_PEPPER = "edp-identifier-hash-pepper".toByteStringUtf8()
private val EDP_HKDF_PEPPER = "edp-hkdf-pepper".toByteStringUtf8()
private val EDP_EVENT_DATA_DICTIONARY = "some-dictionary".toByteStringUtf8()

@RunWith(JUnit4::class)
class FullWorkflowTest : AbstractInProcessPanelMatchIntegrationTest() {
  override val exchangeWorkflowResourcePath: String = "config/full_exchange_workflow.textproto"

  override val initialDataProviderInputs: Map<String, ByteString> =
    mapOf(
      "edp-identifier-hash-pepper" to EDP_IDENTIFIER_HASH_PEPPER,
      "edp-commutative-deterministic-key" to EDP_COMMUTATIVE_DETERMINISTIC_KEY,
      "edp-encrypted-event-data" to ByteString.EMPTY,
      "edp-event-data-dictionary" to EDP_EVENT_DATA_DICTIONARY,
      "edp-hkdf-pepper" to EDP_HKDF_PEPPER,
    )

  override val initialModelProviderInputs: Map<String, ByteString> =
    mapOf("mp-plaintext-join-keys" to PLAINTEXT_JOIN_KEYS.toByteString())

  override fun validateFinalState(
    dataProviderDaemon: ExchangeWorkflowDaemonForTest,
    modelProviderDaemon: ExchangeWorkflowDaemonForTest
  ) {
    val blob = modelProviderDaemon.readPrivateBlob("mp-decrypted-join-keys")
    Truth.assertThat(blob).isNotNull()
    JoinKeyAndIdCollection.parseFrom(blob) // Does not throw.
  }
}
