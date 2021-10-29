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

#include "wfa/panelmatch/client/joinkeyexchange/join_key_cryptor.h"

#include <algorithm>
#include <string>
#include <utility>
#include <vector>

#include "absl/algorithm/container.h"
#include "absl/memory/memory.h"
#include "absl/status/statusor.h"
#include "absl/strings/str_cat.h"
#include "absl/types/span.h"
#include "common_cpp/macros/macros.h"
#include "tink/util/secret_data.h"
#include "wfa/panelmatch/common/crypto/deterministic_commutative_cipher.h"
#include "wfa/panelmatch/common/crypto/ec_commutative_cipher_key_generator.h"
#include "wfa/panelmatch/common/crypto/key_loader.h"

namespace wfa::panelmatch::client::joinkeyexchange {
namespace {
using ::crypto::tink::util::SecretData;
using ::crypto::tink::util::SecretDataAsStringView;
using ::crypto::tink::util::SecretDataFromStringView;
using ::google::protobuf::RepeatedPtrField;
using ::wfa::panelmatch::common::crypto::DeterministicCommutativeCipher;
using ::wfa::panelmatch::common::crypto::EcCommutativeCipherKeyGenerator;
using ::wfa::panelmatch::common::crypto::LoadKey;
using ::wfa::panelmatch::common::crypto::NewDeterministicCommutativeCipher;

}  // namespace

absl::StatusOr<JoinKeyCryptorGenerateCipherKeyResponse>
JoinKeyGenerateCipherKey(
    const JoinKeyCryptorGenerateCipherKeyRequest& request) {
  EcCommutativeCipherKeyGenerator generator;
  ASSIGN_OR_RETURN(SecretData key, generator.GenerateKey());
  JoinKeyCryptorGenerateCipherKeyResponse response;
  response.set_key(std::string(SecretDataAsStringView(key)).data());
  return response;
}

absl::StatusOr<JoinKeyCryptorEncryptResponse> JoinKeyEncrypt(
    const JoinKeyCryptorEncryptRequest& request) {
  ASSIGN_OR_RETURN(SecretData key, LoadKey(request.encryption_key()));
  ASSIGN_OR_RETURN(auto cipher, NewDeterministicCommutativeCipher(key));

  JoinKeyCryptorEncryptResponse response;
  for (JoinKeyAndId unprocessed_join_key_and_id :
       request.plaintext_join_key_and_ids()) {
    JoinKeyAndId processed_join_key_and_id;
    processed_join_key_and_id.mutable_join_key_identifier()->set_id(
        unprocessed_join_key_and_id.join_key_identifier().id());
    ASSIGN_OR_RETURN(
        std::string encrypted_join_key,
        cipher->Encrypt(unprocessed_join_key_and_id.join_key().key()));
    processed_join_key_and_id.mutable_join_key()->set_key(encrypted_join_key);
    *response.add_encrypted_join_key_and_ids() =
        std::move(processed_join_key_and_id);
  }

  return response;
}

absl::StatusOr<JoinKeyCryptorDecryptResponse> JoinKeyDecrypt(
    const JoinKeyCryptorDecryptRequest& request) {
  ASSIGN_OR_RETURN(SecretData key, LoadKey(request.encryption_key()));
  ASSIGN_OR_RETURN(auto cipher, NewDeterministicCommutativeCipher(key));

  JoinKeyCryptorDecryptResponse response;
  for (JoinKeyAndId unprocessed_join_key_and_id :
       request.encrypted_join_key_and_ids()) {
    JoinKeyAndId processed_join_key_and_id;
    processed_join_key_and_id.mutable_join_key_identifier()->set_id(
        unprocessed_join_key_and_id.join_key_identifier().id());
    ASSIGN_OR_RETURN(
        std::string encrypted_join_key,
        cipher->Decrypt(unprocessed_join_key_and_id.join_key().key()));
    processed_join_key_and_id.mutable_join_key()->set_key(encrypted_join_key);
    *response.add_decrypted_join_key_and_ids() =
        std::move(processed_join_key_and_id);
  }

  return response;
}

absl::StatusOr<JoinKeyCryptorReEncryptResponse> JoinKeyReEncrypt(
    const JoinKeyCryptorReEncryptRequest& request) {
  ASSIGN_OR_RETURN(SecretData key, LoadKey(request.encryption_key()));
  ASSIGN_OR_RETURN(auto cipher, NewDeterministicCommutativeCipher(key));

  JoinKeyCryptorReEncryptResponse response;
  for (JoinKeyAndId unprocessed_join_key_and_id :
       request.encrypted_join_key_and_ids()) {
    JoinKeyAndId processed_join_key_and_id;
    processed_join_key_and_id.mutable_join_key_identifier()->set_id(
        unprocessed_join_key_and_id.join_key_identifier().id());
    ASSIGN_OR_RETURN(
        std::string encrypted_join_key,
        cipher->ReEncrypt(unprocessed_join_key_and_id.join_key().key()));
    processed_join_key_and_id.mutable_join_key()->set_key(encrypted_join_key);
    *response.add_encrypted_join_key_and_ids() =
        std::move(processed_join_key_and_id);
  }

  return response;
}

}  // namespace wfa::panelmatch::client::joinkeyexchange
