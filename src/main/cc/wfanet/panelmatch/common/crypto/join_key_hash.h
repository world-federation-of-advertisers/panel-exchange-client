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

#ifndef SRC_MAIN_CC_WFANET_PANELMATCH_COMMON_CRYPTO_JOIN_KEY_HASH_H_
#define SRC_MAIN_CC_WFANET_PANELMATCH_COMMON_CRYPTO_JOIN_KEY_HASH_H_

namespace wfanet::panelmatch::common::crypto {

#include "absl/status/statusor.h"
#include "absl/strings/string_view.h"

// Implements a subclass of Fingerprinter, PepperedFingerprinter
class JoinKeyHash {
 public:
  virtual ~JoinKeyHash() = default;
  // Concatenates a pepper to the identifier then hashes it with a Sha256
  // hash function
  virtual absl::StatusOr<uint64_t> Hash(absl::string_view identifier) = 0;
};

}  // namespace wfanet::panelmatch::common::crypto

#endif  // SRC_MAIN_CC_WFANET_PANELMATCH_COMMON_CRYPTO_JOIN_KEY_HASH_H_
