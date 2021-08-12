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

import java.nio.file.Paths
import org.wfanet.panelmatch.common.loadLibrary
import org.wfanet.panelmatch.common.wrapJniException

/**
 * [QueryEvaluator] that calls into C++ via JNI.
 *
 * TODO: once [QueryEvaluatorParameters] are non-trivial, add a constructor argument.
 */
class JniQueryEvaluator : QueryEvaluator {

  override fun executeQueries(
    shards: List<DatabaseShard>,
    queryBundles: List<QueryBundle>
  ): List<Result> {
    val request =
      ExecuteQueriesRequest.newBuilder()
        .also {
          it.addAllShards(shards)
          it.addAllQueryBundles(queryBundles)
        }
        .build()

    val response: ExecuteQueriesResponse = wrapJniException {
      ExecuteQueriesResponse.parseFrom(
        QueryEvaluatorWrapper.executeQueriesWrapper(request.toByteArray())
      )
    }
    return response.resultList
  }

  override fun combineResults(results: Sequence<Result>): Result {
    val request =
      CombineResultsRequest.newBuilder().also { it.addAllResults(results.asIterable()) }.build()

    val response: CombineResultsResponse = wrapJniException {
      CombineResultsResponse.parseFrom(
        QueryEvaluatorWrapper.combineResultsWrapper(request.toByteArray())
      )
    }

    return response.result
  }

  companion object {
    const val SWIG_BASE = "panel_exchange_client/src/main/swig/wfanet/panelmatch"
    init {
      loadLibrary(
        name = "query_evaluator",
        directoryPath = Paths.get("$SWIG_BASE/client/batchlookup/queryevaluator")
      )
    }
  }
}
