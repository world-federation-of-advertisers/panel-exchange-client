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

#include "wfa/panelmatch/client/keygeneration/key_generator.h"

#include <memory>

#include "crypto/ec_commutative_cipher.h"
#include "openssl/rand.h"
#include "wfa/panelmatch/common/crypto/cryptor.h"

namespace wfa::panelmatch::client::keygeneration {

using ::crypto::tink::util::SecretData;
using ::crypto::tink::util::SecretDataFromStringView;
using ::private_join_and_compute::ECCommutativeCipher;

absl::StatusOr<GeneratedKeys> KeyGenerator::GenerateKeys() const {
  GeneratedKeys keys;
  uint8_t pepper[32];
  RAND_bytes(pepper, sizeof(keys.pepper));
  keys.pepper = SecretDataFromStringView(reinterpret_cast<char*>(pepper));
  ASSIGN_OR_RETURN(
      std::unique_ptr<ECCommutativeCipher> cipher,
      ECCommutativeCipher::CreateWithNewKey(
          NID_X9_62_prime256v1, ECCommutativeCipher::HashType::SHA256));
  keys.cryptokey = SecretDataFromStringView(cipher->GetPrivateKeyBytes());

  return keys;
}

}  // namespace wfa::panelmatch::client::keygeneration
