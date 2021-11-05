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

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.toByteStringUtf8
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.measurement.common.flatten
import org.wfanet.measurement.storage.testing.InMemoryStorageClient
import org.wfanet.panelmatch.client.joinkeyexchange.joinKeyAndIdCollection
import org.wfanet.panelmatch.client.joinkeyexchange.testing.PLAINTEXT_JOIN_KEYS
import org.wfanet.panelmatch.client.privatemembership.JniQueryPreparer
import org.wfanet.panelmatch.client.privatemembership.LookupKeyAndId
import org.wfanet.panelmatch.client.privatemembership.LookupKeyAndIdCollection
import org.wfanet.panelmatch.common.storage.createBlob

private const val ATTEMPT_KEY = "some-arbitrary-attempt-key"

// TODO(@stevenwarejones): clean up these tests:
//   1. Only use `withContext` when we actually care about testing the specific behavior of that

@RunWith(JUnit4::class)
class JoinKeyHashingExchangeTaskTest {
  private val mockStorage = InMemoryStorageClient()
  private val queryPreparer = JniQueryPreparer()
  private val pepper = "some-secret-salt-1".toByteStringUtf8()
  private val saltedJoinKeys = queryPreparer.prepareLookupKeys(pepper, PLAINTEXT_JOIN_KEYS)

  private val blobOfPepper = runBlocking { mockStorage.createBlob("mp-pepper", pepper) }
  private val blobOfJoinKeys = runBlocking {
    mockStorage.createBlob(
      "hashed-join-keys",
      joinKeyAndIdCollection { joinKeysAndIds += PLAINTEXT_JOIN_KEYS }.toByteString()
    )
  }

  @Test
  fun `hash inputs`() = withTestContext {
    val result =
      JoinKeyHashingExchangeTask.forHashing(queryPreparer)
        .execute(mapOf("pepper" to blobOfPepper, "join-keys" to blobOfJoinKeys))
    assertThat(parseResults(result.getValue("hashed-join-keys").flatten()))
      .containsExactlyElementsIn(saltedJoinKeys)
  }
}

private fun withTestContext(block: suspend () -> Unit) {
  runBlocking { withContext(CoroutineName(ATTEMPT_KEY) + Dispatchers.Default) { block() } }
}

private fun parseResults(cryptoResult: ByteString): List<LookupKeyAndId> {
  return LookupKeyAndIdCollection.parseFrom(cryptoResult).lookupKeyAndIdsList
}
