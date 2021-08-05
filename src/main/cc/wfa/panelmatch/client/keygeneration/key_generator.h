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

#ifndef SRC_MAIN_CC_WFA_PANELMATCH_CLIENT_KEYGENERATION_KEY_GENERATOR_H_
#define SRC_MAIN_CC_WFA_PANELMATCH_CLIENT_KEYGENERATION_KEY_GENERATOR_H_

#include "absl/status/statusor.h"
#include "tink/util/secret_data.h"

namespace wfa::panelmatch::client::keygeneration {

struct GeneratedKeys {
  crypto::tink::util::SecretData pepper;
  crypto::tink::util::SecretData cryptokey;
};

class KeyGenerator {
 public:
  KeyGenerator() = default;
  ~KeyGenerator() = default;

  absl::StatusOr<GeneratedKeys> GenerateKeys() const;
};

}  // namespace wfa::panelmatch::client::keygeneration

#endif  // SRC_MAIN_CC_WFA_PANELMATCH_CLIENT_KEYGENERATION_KEY_GENERATOR_H_
