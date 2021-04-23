// Copyright 2020 The Cross-Media Measurement Authors
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

#ifndef SRC_MAIN_CC_WFANET_PANELMATCH_COMMON_CRYPTO_CRYPTOR_H_
#define SRC_MAIN_CC_WFANET_PANELMATCH_COMMON_CRYPTO_CRYPTOR_H_

#include <memory>
#include <string>

#include "absl/status/status.h"
#include "absl/status/statusor.h"
#include "absl/strings/string_view.h"
#include "absl/types/span.h"

#include "crypto/ec_commutative_cipher.h"

namespace wfanet::panelmatch::common::crypto {
using ::private_join_and_compute::ECCommutativeCipher;
// A cryptor dealing with basic operations needed for panel match
class Cryptor {
 public:
  virtual ~Cryptor() = default;

  Cryptor(Cryptor&& other) = delete;
  Cryptor& operator=(Cryptor&& other) = delete;
  Cryptor(const Cryptor&) = delete;
  Cryptor& operator=(const Cryptor&) = delete;

  virtual absl::StatusOr<std::string> Decrypt(
      absl::string_view encrypted_string) = 0;
  virtual absl::StatusOr<std::string> Encrypt(
      absl::string_view plaintext) = 0;
  virtual absl::StatusOr<std::string> ReEncrypt(
      absl::string_view encrypted_string) = 0;

 protected:
  Cryptor() = default;
};

// Create a Cryptor.
absl::StatusOr<std::unique_ptr<Cryptor>> CreateCryptorWithNewKey(void);
absl::StatusOr<std::unique_ptr<Cryptor>> CreateCryptorFromKey(absl::string_view key_bytes);

}  // namespace wfanet::panelmatch::common::crypto

#endif // SRC_MAIN_CC_WFANET_PANELMATCH_COMMON_CRYPTO_PROTOCOL_CRYPTOR_H_
