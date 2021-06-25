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

#ifndef SRC_MAIN_CC_WFANET_PANELMATCH_COMMON_CRYPTO_AES_H_
#define SRC_MAIN_CC_WFANET_PANELMATCH_COMMON_CRYPTO_AES_H_

namespace wfanet::panelmatch::common::crypto {

#include <string>

#include "absl/status/statusor.h"
#include "absl/strings/string_view.h"

// An interface to ensure the Aes scheme used has an encrypt and decrypt method
// that takes a key and an input
class Aes {
 public:
  virtual ~Aes() = default;
  // An encryption method that uses an AES Key to encrypt an input
  virtual absl::StatusOr<std::string> Encrypt(absl::string_view input,
                                              absl::string_view key) = 0;
  // A decryption method that uses an AES Key to decrypt an input
  virtual absl::StatusOr<std::string> Decrypt(absl::string_view input,
                                              absl::string_view key) = 0;
};

}  // namespace wfanet::panelmatch::common::crypto

#endif  // SRC_MAIN_CC_WFANET_PANELMATCH_COMMON_CRYPTO_AES_H_
