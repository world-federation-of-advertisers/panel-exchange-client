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

#include "wfa/panelmatch/client/eventpreprocessing/preprocess_events.h"

#include <wfa/panelmatch/protocol/crypto/event_data_preprocessor.h>

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
#include "src/main/cc/wfa/panelmatch/protocol/crypto/event_data_preprocessor.h"
#include "wfa/panelmatch/client/eventpreprocessing/preprocess_events.pb.h"

class EventDataPreprocessor;
namespace wfa::panelmatch::client {
namespace {
using ::testing::ContainerEq;
using ::testing::Eq;
using ::testing::Ne;
using ::testing::Not;
using ::testing::Pointwise;
using ::wfa::panelmatch::protocol::crypto::EventDataPreprocessor;
using ::wfa::panelmatch::protocol::crypto::ProcessedData;

PreprocessEventsRequest makeTestRequest(const char* id, const char* data,
                                        const char* cryptokey,
                                        const char* pepper) {
  PreprocessEventsRequest request;
  PreprocessEventsRequest::UnprocessedEvent* unprocessed_event =
      request.add_unprocessed_events();
  unprocessed_event->set_id(id);
  unprocessed_event->set_data("some-identifier");
  request.set_crypto_key("test_crypto_key");
  request.set_pepper("test_pepper");
  return request;
}

// Test using actual implementations to ensure nothing crashes
TEST(EventDataPreprocessorTests, actualValues) {
  PreprocessEventsRequest test_request = makeTestRequest(
      "some-identifier", "some-data", "test_crypto_key", "test_pepper");

  ASSERT_OK_AND_ASSIGN(PreprocessEventsResponse processed,
                       PreprocessEvents(test_request));
}

}  // namespace
}  // namespace wfa::panelmatch::client
