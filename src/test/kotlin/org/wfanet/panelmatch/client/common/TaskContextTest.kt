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

package org.wfanet.panelmatch.client.common

import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFails
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

data class TestDataClass1(val someParameter: Int)

data class TestDataClass2(val someOtherParameter: Int)

class InvalidClass1(val yetAnotherParameter: Int)

@RunWith(JUnit4::class)
class TaskContextTest {

  @Test
  fun readAndWrite() {
    val parameters =
      TaskParameters().apply {
        put(TestDataClass1(1))
        put(TestDataClass2(2))
      }
    val parameter = requireNotNull(parameters.get(TestDataClass1::class))
    assertThat(parameter).isInstanceOf(TestDataClass1::class.java)
    assertThat(parameter.someParameter).isEqualTo(1)
  }

  @Test
  fun readEmptyKey() {
    val parameters = TaskParameters()
    parameters.put(TestDataClass1(1))
    assertThat(parameters.get(TestDataClass2::class)).isEqualTo(null)
  }

  @Test
  fun writeSameKey2x() {
    val parameters = TaskParameters()
    parameters.put(TestDataClass1(1))
    assertFails { parameters.put(TestDataClass1(2)) }
  }

  @Test
  fun readInvalidKey() {
    assertFails { TaskParameters().get(InvalidClass1::class) }
  }

  @Test
  fun writeInvalidKey() {
    assertFails { TaskParameters().put(InvalidClass1(1)) }
  }
}
