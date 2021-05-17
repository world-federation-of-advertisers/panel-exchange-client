// Copyright 2020 The Cross-Media Measurement Authors
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

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.panelmatch.protocol.common.applyCommutativeDecryption
import org.wfanet.panelmatch.protocol.common.applyCommutativeEncryption
import org.wfanet.panelmatch.protocol.common.reApplyCommutativeEncryption
import wfanet.panelmatch.protocol.protobuf.SharedInputs

@RunWith(JUnit4::class)
class ExchangeTasksTest {

  @Test
  fun `test encrypt reencrypt and decrypt tasks`() = runBlocking {
    val joinKeys =
      listOf<ByteString>(
        ByteString.copyFromUtf8("some key0"),
        ByteString.copyFromUtf8("some key1"),
        ByteString.copyFromUtf8("some key2"),
        ByteString.copyFromUtf8("some key3"),
        ByteString.copyFromUtf8("some key4")
      )
    val randomCommutativeDeterministicKey1: ByteString = ByteString.copyFromUtf8("random-key-000")
    val randomCommutativeDeterministicKey2: ByteString = ByteString.copyFromUtf8("random-key-222")
    val fakeSendDebugLog: suspend (String) -> Unit = {}

    val singleBlindedOutputs: Map<String, ByteString> =
      SingleBlindJoinKeysTask()
        .execute(
          mapOf(
            "commutative-deterministic-key" to randomCommutativeDeterministicKey1,
            "raw-joinkeys" to SharedInputs.newBuilder().addAllData(joinKeys).build().toByteString()
          ),
          fakeSendDebugLog
        )
    println(singleBlindedOutputs)
    assertThat(
        SharedInputs.parseFrom(singleBlindedOutputs["single-blinded-joinkeys"]).getDataList()
      )
      .isEqualTo(applyCommutativeEncryption(randomCommutativeDeterministicKey1, joinKeys))

    val doubleBlindedOutputs: Map<String, ByteString> =
      DoubleBlindJoinKeysTask()
        .execute(
          mapOf(
            "commutative-deterministic-key" to randomCommutativeDeterministicKey2,
            "single-blinded-joinkeys" to singleBlindedOutputs["single-blinded-joinkeys"]!!
          ),
          fakeSendDebugLog
        )
    assertThat(
        SharedInputs.parseFrom(doubleBlindedOutputs["double-blinded-joinkeys"]).getDataList()
      )
      .isEqualTo(
        reApplyCommutativeEncryption(
          randomCommutativeDeterministicKey2,
          applyCommutativeEncryption(randomCommutativeDeterministicKey1, joinKeys)
        )
      )

    val lookupKeysOutputs: Map<String, ByteString> =
      PrepareLookupKeysTask()
        .execute(
          mapOf(
            "commutative-deterministic-key" to randomCommutativeDeterministicKey1,
            "double-blinded-joinkeys" to doubleBlindedOutputs["double-blinded-joinkeys"]!!
          ),
          fakeSendDebugLog
        )
    assertThat(SharedInputs.parseFrom(lookupKeysOutputs["lookup-keys"]).getDataList())
      .isEqualTo(
        applyCommutativeDecryption(
          randomCommutativeDeterministicKey1,
          reApplyCommutativeEncryption(
            randomCommutativeDeterministicKey2,
            applyCommutativeEncryption(randomCommutativeDeterministicKey1, joinKeys)
          )
        )
      )
  }
}
