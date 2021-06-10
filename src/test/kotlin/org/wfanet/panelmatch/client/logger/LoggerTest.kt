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

package org.wfanet.panelmatch.client.logger

import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import org.junit.Test

class JobTestClass1 {
  private val LOGGER = loggerFor(javaClass)
  suspend fun logWithDelay() {
    LOGGER.addToTaskLog("logWithDelay: Log Message 0")
    delay(100)
    LOGGER.addToTaskLog("logWithDelay: Log Message 1")
  }
}

class JobTestClass2 {
  private val LOGGER = loggerFor(javaClass)
  suspend fun logWithDelay() {
    LOGGER.addToTaskLog("logWithDelay: Log Message 0")
    delay(100)
    LOGGER.addToTaskLog("logWithDelay: Log Message 1")
  }
}

class LoggerTest {
  private val LOGGER = loggerFor(javaClass)

  @Test
  fun `write single task log from coroutine and suspend function`() = runBlocking {
    val ATTEMPT_KEY = java.util.UUID.randomUUID().toString()
    val job =
      async(CoroutineName(ATTEMPT_KEY) + Dispatchers.Default) {
        LOGGER.addToTaskLog("Log Message 0")
        JobTestClass1().logWithDelay()
        val log = LOGGER.getAndClearTaskLog()
        assertThat(log).hasSize(3)
        log.forEach { assertThat(it).contains(ATTEMPT_KEY) }
      }
    job.join()
  }

  @Test
  fun `multiple jobs have separate task logs`() = runBlocking {
    val ATTEMPT_KEY_0 = java.util.UUID.randomUUID().toString()
    val ATTEMPT_KEY_1 = java.util.UUID.randomUUID().toString()
    val job1 =
      async(CoroutineName(ATTEMPT_KEY_0) + Dispatchers.Default) {
        LOGGER.addToTaskLog("Log Message 0")
        JobTestClass1().logWithDelay()
        val log = LOGGER.getAndClearTaskLog()
        assertThat(log).hasSize(3)
        log.forEach { assertThat(it).contains(ATTEMPT_KEY_0) }
      }
    val job2 =
      async(CoroutineName(ATTEMPT_KEY_1) + Dispatchers.Default) {
        JobTestClass1().logWithDelay()
        val log = LOGGER.getAndClearTaskLog()
        assertThat(log).hasSize(2)
        log.forEach { assertThat(it).contains(ATTEMPT_KEY_1) }
      }
    val jobs = listOf(job1, job2)
    jobs.joinAll()
  }

  @Test
  fun `cannot add to a task log unless you are in a job`() = runBlocking {
    val outsideCoroutineException =
      assertFailsWith(IllegalArgumentException::class) { LOGGER.addToTaskLog("Log Message 0") }
  }
}
