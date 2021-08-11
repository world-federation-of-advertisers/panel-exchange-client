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

package org.wfanet.panelmatch.client.batchlookup

import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.panelmatch.common.JniException

@RunWith(JUnit4::class)
class JniObliviousQueryBuilderTest {
  val obliviousQueryBuilder: ObliviousQueryBuilder = JniObliviousQueryBuilder()

  @Test
  fun `invalid proto throws JniException`() {
    val missingKeyException =
      assertFailsWith(JniException::class) {
        val request = EncryptQueriesRequest.getDefaultInstance()
        obliviousQueryBuilder.encryptQueries(request)
      }
    assertThat(missingKeyException.message).contains("UNIMPLEMENTED: Not implemented")
  }
}
