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

#include <string>

#include "common_cpp/testing/status_macros.h"
#include "common_cpp/testing/status_matchers.h"
#include "gtest/gtest.h"
#include "wfa/panelmatch/client/privatemembership/prepare_query.pb.h"
#include "wfa/panelmatch/client/privatemembership/query_preparer.h"
#include "wfa/panelmatch/client/privatemembership/query_preparer_wrapper.h"

namespace wfa::panelmatch::client::privatemembership {
namespace {

using wfa::panelmatch::client::exchangetasks::JoinKeyAndId;

TEST(PrepareQuery, PrepareQueryTest) {
  PrepareQueryRequest test_request;
  test_request.set_identifier_hash_pepper("some-pepper");
  JoinKeyAndId decrypted_join_key_and_id_1;
  decrypted_join_key_and_id_1.mutable_join_key()->set_key("some joinkey0");
  decrypted_join_key_and_id_1.mutable_join_key_identifier()->set_id(
      "some identifier0");
  *test_request.add_decrypted_join_key_and_ids() = decrypted_join_key_and_id_1;
  JoinKeyAndId decrypted_join_key_and_id_2;
  decrypted_join_key_and_id_1.mutable_join_key()->set_key("some joinkey1");
  decrypted_join_key_and_id_1.mutable_join_key_identifier()->set_id(
      "some identifier1");
  *test_request.add_decrypted_join_key_and_ids() = decrypted_join_key_and_id_2;
  absl::StatusOr<PrepareQueryResponse> test_response =
      PrepareQuery(test_request);
  EXPECT_THAT(test_response.status(), IsOk());

  std::string valid_serialized_request;
  test_request.SerializeToString(&valid_serialized_request);
  absl::StatusOr<std::string> wrapper_test_response1 =
      PrepareQueryWrapper(valid_serialized_request);
  EXPECT_THAT(wrapper_test_response1.status(), IsOk());

  absl::StatusOr<std::string> wrapper_test_response2 =
      PrepareQueryWrapper("some-invalid-serialized-request");
  EXPECT_THAT(wrapper_test_response2.status(),
              StatusIs(absl::StatusCode::kInternal, ""));
}

}  // namespace
}  // namespace wfa::panelmatch::client::privatemembership
