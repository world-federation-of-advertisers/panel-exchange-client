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

#include <iostream>
#include <memory>
#include <string>

#include "common_cpp/macros/macros.h"
#include "tink/util/secret_data.h"
#include "wfa/panelmatch/common/crypto/aes.h"
#include "wfa/panelmatch/common/crypto/aes_with_hkdf.h"
#include "wfa/panelmatch/common/crypto/hkdf.h"

namespace wfa::panelmatch::client::eventpreprocessing::tools {

using ::crypto::tink::util::SecretDataFromStringView;
using ::wfa::panelmatch::common::crypto::Aes;
using ::wfa::panelmatch::common::crypto::AesWithHkdf;
using ::wfa::panelmatch::common::crypto::GetAesSivCmac512;
using ::wfa::panelmatch::common::crypto::GetSha256Hkdf;
using ::wfa::panelmatch::common::crypto::Hkdf;

// Spot check AesWithHkdf enrypted values from the command line
// Parameters: encrypted value, key
int main(int argc, char** argv) {
  if (argc != 3) {
    std::cout << "There must be 2 parameters" << std::endl;
    return 1;
  }

  std::unique_ptr<Hkdf> hkdf = GetSha256Hkdf();
  std::unique_ptr<Aes> aes = GetAesSivCmac512();
  AesWithHkdf aes_hkdf(std::move(hkdf), std::move(aes));

  absl::StatusOr<std::string> plaintext =
      aes_hkdf.Decrypt(argv[1], SecretDataFromStringView(argv[2]));
  if (!plaintext.ok()) {
    std::cout << "Decryption failed" << std::endl;
    return 1;
  }

  std::cout << "Decrypted value: " << plaintext.value() << std::endl;
  return 0;
}

}  // namespace wfa::panelmatch::client::eventpreprocessing::tools
