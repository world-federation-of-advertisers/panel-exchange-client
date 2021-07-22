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

#include "wfa/panelmatch/protocol/crypto/event_data_preprocessor.h"

#include "common_cpp/fingerprinters/fingerprinters.h"
#include "common_cpp/testing/status_macros.h"
#include "include/gtest/gtest.h"
#include "tink/util/secret_data.h"
#include "wfa/panelmatch/common/crypto/aes.h"
#include "wfa/panelmatch/common/crypto/aes_with_hkdf.h"
#include "wfa/panelmatch/common/crypto/cryptor.h"
#include "wfa/panelmatch/common/crypto/hkdf.h"

namespace wfa::panelmatch::protocol::crypto {
namespace {

using common::crypto::Aes;
using common::crypto::AesWithHkdf;
using common::crypto::Cryptor;
using common::crypto::Hkdf;
using ::crypto::tink::util::SecretData;
using ::crypto::tink::util::SecretDataFromStringView;

// Fake Hkdf class for testing purposes only
class FakeHkdf : public Hkdf {
 public:
  FakeHkdf() = default;

  absl::StatusOr<SecretData> ComputeHkdf(const SecretData& ikm,
                                         int length) const override {
    return SecretDataFromStringView("hkdf");
  }
};

// Fake Aes class for testing purposes only
class FakeAes : public Aes {
 public:
  FakeAes() = default;

  absl::StatusOr<std::string> Encrypt(absl::string_view input,
                                      const SecretData& key) const override {
    return "Encrypted";
  }

  absl::StatusOr<std::string> Decrypt(absl::string_view input,
                                      const SecretData& key) const override {
    return "Decrypted";
  }

  int32_t key_size_bytes() const override { return 64; }
};

// Fake Fingerprinter class for testing purposes only
class FakeFingerprinter : public wfa::Fingerprinter {
 public:
  ~FakeFingerprinter() override = default;
  FakeFingerprinter() = default;

  uint64_t Fingerprint(absl::Span<const unsigned char> item) const override {
    return 1;
  }
};

// Fake Cryptor class for testing purposes only
class FakeCryptor : public Cryptor {
 public:
  FakeCryptor() = default;
  ~FakeCryptor() = default;
  FakeCryptor(FakeCryptor&& other) = delete;
  FakeCryptor& operator=(FakeCryptor&& other) = delete;
  FakeCryptor(const FakeCryptor&) = delete;
  FakeCryptor& operator=(const FakeCryptor&) = delete;

  absl::StatusOr<std::vector<std::string>> BatchProcess(
      const std::vector<std::string>& plaintexts_or_ciphertexts,
      wfa::panelmatch::common::crypto::Action action) override {
    return plaintexts_or_ciphertexts;
  }

  absl::StatusOr<google::protobuf::RepeatedPtrField<std::string>> BatchProcess(
      const google::protobuf::RepeatedPtrField<std::string>&
          plaintexts_or_ciphertexts,
      wfa::panelmatch::common::crypto::Action action) override {
    return plaintexts_or_ciphertexts;
  }
};

std::unique_ptr<FakeCryptor> CreateFakeCryptor(void) {
  return absl::make_unique<FakeCryptor>();
}

// Test using fake classes to ensure proper return values
TEST(EventDataPreprocessorTests, properImplementation) {
  const FakeFingerprinter fingerprinter = FakeFingerprinter();
  std::unique_ptr<Hkdf> hkdf = absl::make_unique<FakeHkdf>();
  std::unique_ptr<Aes> aes = absl::make_unique<FakeAes>();
  const AesWithHkdf aes_hkdf =
      common::crypto::AesWithHkdf(std::move(hkdf), std::move(aes));
  std::unique_ptr<FakeCryptor> cryptor = CreateFakeCryptor();
  EventDataPreprocessor preprocessor(std::move(cryptor),
                                     SecretDataFromStringView("pepper"),
                                     &fingerprinter, aes_hkdf);
  ASSERT_OK_AND_ASSIGN(ProcessedData processed,
                       preprocessor.Process("identifier", "event"));
  ASSERT_EQ(processed.encrypted_identifier, 1);
  ASSERT_EQ(processed.encrypted_event_data, "Encrypted");
}

// Tests EventDataPreprocessor with null Fingerprinter
TEST(EventDataPreprocessorTests, nullFingerpritner) {
  std::unique_ptr<Hkdf> hkdf = absl::make_unique<FakeHkdf>();
  std::unique_ptr<Aes> aes = absl::make_unique<FakeAes>();
  const AesWithHkdf aes_hkdf =
      common::crypto::AesWithHkdf(std::move(hkdf), std::move(aes));
  std::unique_ptr<FakeCryptor> cryptor = CreateFakeCryptor();
  ASSERT_DEATH(EventDataPreprocessor preprocessor(
                   std::move(cryptor), SecretDataFromStringView("pepper"),
                   nullptr, aes_hkdf),
               "");
}

// Test using actual implementations to ensure nothing crashes
TEST(EventDataPreprocessorTests, actualValues) {
  const Fingerprinter& sha = GetSha256Fingerprinter();
  std::unique_ptr<Hkdf> hkdf = common::crypto::GetSha256Hkdf();
  std::unique_ptr<Aes> aes = common::crypto::GetAesSivCmac512();
  const AesWithHkdf aes_hkdf =
      common::crypto::AesWithHkdf(std::move(hkdf), std::move(aes));
  ASSERT_OK_AND_ASSIGN(std::unique_ptr<Cryptor> cryptor,
                       common::crypto::CreateCryptorWithNewKey());
  EventDataPreprocessor preprocessor(
      std::move(cryptor), SecretDataFromStringView("pepper"), &sha, aes_hkdf);
  ASSERT_OK_AND_ASSIGN(ProcessedData processed,
                       preprocessor.Process("identifier", "event"));
}

}  // namespace
}  // namespace wfa::panelmatch::protocol::crypto
