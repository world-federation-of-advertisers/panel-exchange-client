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

#include "wfanet/panelmatch/common/crypto/aes.h"
// #include "tink/cc/subtle/aes_siv_boringssl.h"
#include "absl/status/status.h"

namespace wfanet::panelmatch::common::crypto {
namespace {

using ::crypto::tink::util::SecretData;

class AesSiv : public Aes {
 public:
  AesSiv() = default;

  absl::StatusOr<std::string> Encrypt(absl::string_view input,
                                      const SecretData& key) const override {
    //    AesSivBoringSsl implementation = new AesSivBoringSsl(key);
    //    return implementation.EncryptDeterministically(input, "");

    return absl::UnimplementedError("Not implemented");
  }

  absl::StatusOr<std::string> Decrypt(absl::string_view input,
                                      const SecretData& key) const override {
    //    AesSivBoringSsl implementation = new AesSivBoringSsl(key);
    //    return implementation.DecryptDeterministically(input, "");

    return absl::UnimplementedError("Not implemented");
  }
};
}  // namespace

const Aes& GetAesSiv() {
  static const auto* const aes = new AesSiv();
  return *aes;
}
}  // namespace wfanet::panelmatch::common::crypto
