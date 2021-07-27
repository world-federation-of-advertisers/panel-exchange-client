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

#include "wfa/panelmatch/client/batchlookup/query_evaluator.h"

#include <string>

#include "common_cpp/testing/status_macros.h"
#include "common_cpp/testing/status_matchers.h"
#include "wfa/panelmatch/client/batchlookup/query_evaluator.pb.h"

namespace wfa::panelmatch::client::batchlookup {
namespace {

TEST(QueryEvaluatorTest, ExecuteQueries) {
  ExecuteQueriesRequest request;
  EXPECT_THAT(ExecuteQueries(request).status(),
              StatusIs(absl::StatusCode::kUnimplemented, ""));
}

TEST(QueryEvaluatorTest, CombineResults) {
  CombineResultsRequest request;
  EXPECT_THAT(CombineResults(request).status(),
              StatusIs(absl::StatusCode::kUnimplemented, ""));
}

}  // namespace
}  // namespace wfa::panelmatch::client::batchlookup
