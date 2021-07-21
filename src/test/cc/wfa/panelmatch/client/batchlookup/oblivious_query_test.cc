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

#include "wfa/panelmatch/client/batchlookup/oblivious_query.h"

#include <string>

#include "absl/base/port.h"
#include "absl/memory/memory.h"
#include "absl/status/status.h"
#include "absl/status/statusor.h"
#include "absl/types/span.h"
#include "common_cpp/testing/status_macros.h"
#include "common_cpp/testing/status_matchers.h"
#include "gmock/gmock.h"
#include "google/protobuf/descriptor.h"
#include "google/protobuf/message.h"
#include "gtest/gtest.h"
#include "wfa/panelmatch/client/batchlookup/oblivious_query.pb.h"
#include "wfa/panelmatch/client/batchlookup/oblivious_query_wrapper.h"

namespace wfa::panelmatch::client::batchlookup {
namespace {
using ::testing::ContainerEq;
using ::testing::Eq;
using ::testing::Ne;
using ::testing::Not;
using ::testing::Pointwise;
using ::wfa::panelmatch::client::batchlookup::DecryptQueriesRequest;
using ::wfa::panelmatch::client::batchlookup::EncryptQueriesRequest;
using ::wfa::panelmatch::client::batchlookup::GenerateKeysRequest;
using ::wfa::panelmatch::client::batchlookup::Parameters;
using ::wfa::panelmatch::client::batchlookup::UnencryptedQuery;

TEST(ObliviousQuery, GenerateKeysTest) {
  GenerateKeysRequest test_request;
  auto test_response = GenerateKeys(test_request);
  EXPECT_THAT(test_response.status(),
              StatusIs(absl::StatusCode::kUnimplemented, ""));

  std::string validSerializedRequest;
  test_request.SerializeToString(&validSerializedRequest);
  auto wrapper_test_response1 = GenerateKeysWrapper(validSerializedRequest);
  EXPECT_THAT(wrapper_test_response1.status(),
              StatusIs(absl::StatusCode::kUnimplemented, ""));

  std::string invalidSerializedRequest = "some-invalid-serialized-request";
  auto wrapper_test_response2 = GenerateKeysWrapper(invalidSerializedRequest);
  EXPECT_THAT(wrapper_test_response2.status(),
              StatusIs(absl::StatusCode::kInternal, ""));
}

TEST(ObliviousQuery, EncryptQueriesTest) {
  EncryptQueriesRequest test_request;
  std::string some_public_key = "some-public-key";
  std::string some_private_key = "some-private-key";
  test_request.set_public_key(some_public_key);
  test_request.set_private_key(some_private_key);
  UnencryptedQuery* unencrypted_query = test_request.add_unencrypted_query();
  unencrypted_query->set_shard_id(1);
  unencrypted_query->set_query_id(2);
  unencrypted_query->set_bucket_id(3);
  auto test_response = EncryptQueries(test_request);
  EXPECT_THAT(test_response.status(),
              StatusIs(absl::StatusCode::kUnimplemented, ""));

  std::string validSerializedRequest;
  test_request.SerializeToString(&validSerializedRequest);
  auto wrapper_test_response1 = EncryptQueriesWrapper(validSerializedRequest);
  EXPECT_THAT(wrapper_test_response1.status(),
              StatusIs(absl::StatusCode::kUnimplemented, ""));

  std::string invalidSerializedRequest = "some-invalid-serialized-request";
  auto wrapper_test_response2 = EncryptQueriesWrapper(invalidSerializedRequest);
  EXPECT_THAT(wrapper_test_response2.status(),
              StatusIs(absl::StatusCode::kInternal, ""));
}

TEST(ObliviousQuery, DecryptQueriesTest) {
  DecryptQueriesRequest test_request;
  std::string some_public_key = "some-public-key";
  std::string some_private_key = "some-private-key";
  test_request.set_public_key(some_public_key);
  test_request.set_private_key(some_private_key);
  std::string some_encrypted_query_result = "some-encrypted-query-result";
  test_request.add_encrypted_query_results(some_encrypted_query_result);
  auto test_response = DecryptQueries(test_request);
  EXPECT_THAT(test_response.status(),
              StatusIs(absl::StatusCode::kUnimplemented, ""));

  std::string validSerializedRequest;
  test_request.SerializeToString(&validSerializedRequest);
  auto wrapper_test_response1 = DecryptQueriesWrapper(validSerializedRequest);
  EXPECT_THAT(wrapper_test_response1.status(),
              StatusIs(absl::StatusCode::kUnimplemented, ""));

  std::string invalidSerializedRequest = "some-invalid-serialized-request";
  auto wrapper_test_response2 = DecryptQueriesWrapper(invalidSerializedRequest);
  EXPECT_THAT(wrapper_test_response2.status(),
              StatusIs(absl::StatusCode::kInternal, ""));
}
}  // namespace
}  // namespace wfa::panelmatch::client::batchlookup
