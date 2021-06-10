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

import java.util.logging.Logger
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineName

private val taskLogs: MutableMap<String, MutableList<String>> =
  mutableMapOf<String, MutableList<String>>()

class loggerWrapper(className: String) {

  private val pLogger = Logger.getLogger(className)

  suspend fun addToTaskLog(logMessage: String) {
    val coroutineContextName: String =
      requireNotNull(coroutineContext[CoroutineName.Key]).toString()
    if (coroutineContextName !in taskLogs) {
      taskLogs[coroutineContextName] = mutableListOf<String>()
    }
    requireNotNull(taskLogs[coroutineContextName]).add("${coroutineContextName}:${logMessage}")
    pLogger.info("${coroutineContextName}:${logMessage}")
  }

  fun finest(logMessage: String) {
    pLogger.finest(logMessage)
  }

  fun finer(logMessage: String) {
    pLogger.finer(logMessage)
  }

  fun fine(logMessage: String) {
    pLogger.fine(logMessage)
  }

  fun config(logMessage: String) {
    pLogger.config(logMessage)
  }

  fun info(logMessage: String) {
    pLogger.info(logMessage)
  }

  fun warning(logMessage: String) {
    pLogger.warning(logMessage)
  }

  fun severe(logMessage: String) {
    pLogger.severe(logMessage)
  }

  suspend fun getAndClearTaskLog(): List<String> {
    val taskKey = requireNotNull(coroutineContext[CoroutineName.Key]).toString()
    val listCopy = mutableListOf<String>().apply { addAll(requireNotNull(taskLogs[taskKey])) }
    taskLogs.remove(taskKey)
    return listCopy
  }
}

fun <T> loggerFor(myClass: Class<T>) = loggerWrapper(myClass.getName())
