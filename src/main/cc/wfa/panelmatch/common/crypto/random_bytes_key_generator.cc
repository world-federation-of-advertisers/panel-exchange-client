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

#include "wfa/panelmatch/common/crypto/random_bytes_key_generator.h"

#include "absl/status/statusor.h"
#include "tink/util/secret_data.h"
#include "openssl/rand.h"

namespace wfa::panelmatch::common::crypto {

using ::crypto::tink::util::SecretData;

absl::StatusOr<SecretData> RandomBytesKeyGenerator::GenerateKey() const {
  SecretData key;
  key.resize(32);
  RAND_bytes(key.data(), sizeof(key));
  return key;
}

}  // namespace wfa::panelmatch::common::crypto
