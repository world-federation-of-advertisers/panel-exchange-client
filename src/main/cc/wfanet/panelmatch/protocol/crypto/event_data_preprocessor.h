/*
 * Copyright 2021 The Cross-Media Measurement Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef SRC_MAIN_CC_WFANET_PANELMATCH_PROTOCOL_CRYPTO_EVENT_DATA_PREPROCESSOR_H_
#define SRC_MAIN_CC_WFANET_PANELMATCH_PROTOCOL_CRYPTO_EVENT_DATA_PREPROCESSOR_H_

#include "absl/status/statusor.h"
#include "absl/strings/string_view.h"

namespace wfanet::panelmatch::protocol::crypto {

struct ProcessedData {
  absl::string_view encrypted_identifier;
  absl::string_view encrypted_events;
};

// Implements AES, HKDF, and Sha256 to encrypt event data
class EventDataPreprocessor {
 public:
  virtual ~EventDataPreprocessor() = default;
  // Encrypts 'identifier' and 'event' data
  virtual absl::StatusOr<ProcessedData> Process(absl::string_view identifier,
                                                absl::string_view event) = 0;
};

}  // namespace wfanet::panelmatch::protocol::crypto

#endif  // SRC_MAIN_CC_WFANET_PANELMATCH_PROTOCOL_CRYPTO_EVENT_DATA_PREPROCESSOR_H_
