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
#include "wfa/panelmatch/protocol/crypto/event_data_preprocessor.h"

namespace wfa::panelmatch::client {
namespace {
using ::testing::ContainerEq;
using ::testing::Eq;
using ::testing::Ne;
using ::testing::Not;
using ::testing::Pointwise;
using ::wfa::panelmatch::protocol::crypto::EventDataPreprocessor;
using ::wfa::panelmatch::protocol::crypto::ProcessedData;

// Test using actual implementations to ensure nothing crashes
TEST(EventDataPreprocessorTests, actualValues) {
  PreprocessEventsRequest test_request;
  PreprocessEventsRequest::UnprocessedEvent* unprocessed_event =
      test_request.add_unprocessed_events();
  unprocessed_event->set_id("some-id");
  unprocessed_event->set_data("some-data");
  test_request.set_crypto_key("some-cryptokey");
  test_request.set_pepper("some-pepper");

  ASSERT_OK_AND_ASSIGN(PreprocessEventsResponse processed,
                       PreprocessEvents(test_request));
  ASSERT_EQ(processed.processed_events_size(), 1);
}

TEST(EventDataPreprocessorTests, missingPepper) {
  PreprocessEventsRequest test_request;
  PreprocessEventsRequest::UnprocessedEvent* unprocessed_event =
      test_request.add_unprocessed_events();
  unprocessed_event->set_id("some-id");
  unprocessed_event->set_data("some-data");
  test_request.set_crypto_key("some-cryptokey");
  ASSERT_OK_AND_ASSIGN(PreprocessEventsResponse processed,
                       PreprocessEvents(test_request));
}
TEST(EventDataPreprocessorTests, missingCryptokey) {
  PreprocessEventsRequest test_request;
  PreprocessEventsRequest::UnprocessedEvent* unprocessed_event =
      test_request.add_unprocessed_events();
  unprocessed_event->set_id("some-id");
  unprocessed_event->set_data("some-data");
  test_request.set_pepper("some-pepper");
  ASSERT_THAT(PreprocessEvents(test_request).status(),
              wfa::StatusIs(absl::StatusCode::kInvalidArgument, ""));
}
TEST(EventDataPreprocessorTests, missingUnprocessedEvents) {
  PreprocessEventsRequest test_request;
  test_request.set_crypto_key("some-cryptokey");
  test_request.set_pepper("some-pepper");
  ASSERT_OK_AND_ASSIGN(PreprocessEventsResponse processed,
                       PreprocessEvents(test_request));
  ASSERT_EQ(processed.processed_events_size(), 0);
}
TEST(EventDataPreprocessorTests, missingId) {
  PreprocessEventsRequest test_request;
  PreprocessEventsRequest::UnprocessedEvent* unprocessed_event =
      test_request.add_unprocessed_events();
  unprocessed_event->set_data("some-data");
  test_request.set_pepper("some-pepper");
  test_request.set_crypto_key("some-cryptokey");
  ASSERT_OK_AND_ASSIGN(PreprocessEventsResponse processed,
                       PreprocessEvents(test_request));
}
TEST(EventDataPreprocessorTests, missingData) {
  PreprocessEventsRequest test_request;
  PreprocessEventsRequest::UnprocessedEvent* unprocessed_event =
      test_request.add_unprocessed_events();
  unprocessed_event->set_id("some-id");
  test_request.set_pepper("some-pepper");
  test_request.set_crypto_key("some-cryptokey");
  ASSERT_OK_AND_ASSIGN(PreprocessEventsResponse processed,
                       PreprocessEvents(test_request));
}
TEST(EventDataPreprocessorTests, multipleUnprocessedEvents) {
  PreprocessEventsRequest test_request;
  PreprocessEventsRequest::UnprocessedEvent* unprocessed_event =
      test_request.add_unprocessed_events();
  unprocessed_event->set_id("some-id");
  unprocessed_event->set_data("some-data");
  PreprocessEventsRequest::UnprocessedEvent* unprocessed_event2 =
      test_request.add_unprocessed_events();
  unprocessed_event2->set_id("some-id");
  unprocessed_event2->set_data("some-data");
  PreprocessEventsRequest::UnprocessedEvent* unprocessed_event3 =
      test_request.add_unprocessed_events();
  unprocessed_event3->set_id("some-id");
  unprocessed_event3->set_data("some-data");
  test_request.set_crypto_key("some-cryptokey");
  test_request.set_pepper("some-pepper");

  ASSERT_OK_AND_ASSIGN(PreprocessEventsResponse processed,
                       PreprocessEvents(test_request));
  ASSERT_EQ(processed.processed_events_size(), 3);
}
}  // namespace
}  // namespace wfa::panelmatch::client
