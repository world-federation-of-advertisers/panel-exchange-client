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

#include "wfanet/panelmatch/common/crypto/cryptor.h"

#include <string>
#include <utility>

#include "absl/memory/memory.h"
#include "absl/status/status.h"
#include "absl/status/statusor.h"
#include "absl/synchronization/mutex.h"
#include "crypto/context.h"
#include "crypto/ec_commutative_cipher.h"

namespace wfanet::panelmatch::common::crypto {

namespace {
using ::private_join_and_compute::ECCommutativeCipher;

class CryptorImpl : public Cryptor {
 public:
  CryptorImpl(
    std::unique_ptr<ECCommutativeCipher> local_ec_cipher);
  ~CryptorImpl() override = default;
  CryptorImpl(CryptorImpl&& other) = delete;
  CryptorImpl& operator=(CryptorImpl&& other) = delete;
  CryptorImpl(const CryptorImpl&) = delete;
  CryptorImpl& operator=(const CryptorImpl&) = delete;

  absl::StatusOr<std::string> Decrypt(
      absl::string_view encrypted_string) override;
  absl::StatusOr<std::string> Encrypt(
      absl::string_view plaintext) override;
  absl::StatusOr<std::string> ReEncrypt(
      absl::string_view encrypted_string) override;
  absl::StatusOr<std::vector<std::string>> BatchProcess(
      std::vector<std::string> plaintexts_or_ciphertexts,
      const Action action) override;

 private:
  const std::unique_ptr<ECCommutativeCipher> local_ec_cipher_ GUARDED_BY(mutex_);

  // Since the underlying private-join-and-computer::ECCommuativeCipher is NOT
  // thread safe, we use mutex to enforce thread safety in this class.
  absl::Mutex mutex_; //protects local_ec_cipher_
};

CryptorImpl::CryptorImpl(
    std::unique_ptr<ECCommutativeCipher> local_ec_cipher)
    : local_ec_cipher_(std::move(local_ec_cipher)) {}

absl::StatusOr<std::string> CryptorImpl::Decrypt(
    absl::string_view encrypted_string) LOCKS_EXCLUDED(mutex_) {
  absl::WriterMutexLock l(&mutex_);
  return local_ec_cipher_->Decrypt(encrypted_string);
}

absl::StatusOr<std::string> CryptorImpl::Encrypt(
    absl::string_view plaintext) LOCKS_EXCLUDED(mutex_) {
  absl::WriterMutexLock l(&mutex_);
  return local_ec_cipher_->Encrypt(plaintext);
}

absl::StatusOr<std::string> CryptorImpl::ReEncrypt(
    absl::string_view encrypted_string) LOCKS_EXCLUDED(mutex_) {
  absl::WriterMutexLock l(&mutex_);
  return local_ec_cipher_->ReEncrypt(encrypted_string);
}

absl::StatusOr<std::vector<std::string>> CryptorImpl::BatchProcess(
    std::vector<std::string> plaintexts_or_ciphertexts,
    const Action action) {
  std::vector<std::string> results;

  for (auto &text : plaintexts_or_ciphertexts) {
    switch (action) {
      case Action::Encrypt: {
        ASSIGN_OR_RETURN(auto encrypted_string,
                         Encrypt(text));
        results.push_back(encrypted_string);
        break;
      }
      case Action::ReEncrypt: {
        ASSIGN_OR_RETURN(auto reencrypted_string,
                         ReEncrypt(text));
        results.push_back(reencrypted_string);
        break;
      }
      case Action::Decrypt: {
        ASSIGN_OR_RETURN(auto decrypted_string,
                         Decrypt(text));
        results.push_back(decrypted_string);
        break;
      }
      default:
        return absl::InvalidArgumentError("Unknown action.");
    }
  }
  return std::move(results);
}

}  // namespace

//We probably want to pass in a crypto key in the future. This is just a placeholder.
absl::StatusOr<std::unique_ptr<Cryptor>> CreateCryptorWithNewKey(void) {
  ASSIGN_OR_RETURN(
      auto local_ec_cipher,
      ECCommutativeCipher::CreateWithNewKey(
          NID_X9_62_prime256v1, ECCommutativeCipher::HashType::SHA256));
  return absl::make_unique<CryptorImpl>(
          std::move(local_ec_cipher));
}

absl::StatusOr<std::unique_ptr<Cryptor>> CreateCryptorFromKey(absl::string_view key_bytes) {
  ASSIGN_OR_RETURN(
      auto local_ec_cipher,
      ECCommutativeCipher::CreateFromKey(
          NID_X9_62_prime256v1, key_bytes, ECCommutativeCipher::HashType::SHA256));
  return absl::make_unique<CryptorImpl>(
          std::move(local_ec_cipher));
}

}  // namespace wfanet::panelmatch::common::crypto
