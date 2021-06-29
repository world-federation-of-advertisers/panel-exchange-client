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

#include "wfanet/panelmatch/common/crypto/aes_with_hkdf.h"

#include "absl/strings/escaping.h"
#include "gtest/gtest.h"
#include "src/test/cc/testutil/matchers.h"
#include "src/test/cc/testutil/status_macros.h"

namespace wfanet::panelmatch::common::crypto {
namespace {

using ::crypto::tink::util::SecretData;
using ::crypto::tink::util::SecretDataFromStringView;

// Tests that a value encrypted then decrypted returns that original value
TEST(AesWithHkdfTest, testEncryptDecrypt) {
  std::unique_ptr<Hkdf> hkdf = GetSha256Hkdf();
  std::unique_ptr<Aes> aes = GetAesSivCmac512();
  SecretData key = SecretDataFromStringView("key");
  std::string_view message = "Some data to encrypt.";
  AesWithHkdf aes_hkdf(std::move(hkdf), std::move(aes));
  auto encrypted = aes_hkdf.Encrypt(message, key);
  ASSERT_TRUE(encrypted.ok()) << encrypted.status();
  auto decrypted = aes_hkdf.Decrypt(*encrypted, key);
  ASSERT_TRUE(decrypted.ok()) << decrypted.status();
  EXPECT_EQ(*decrypted, message);
}

// Tests that the result of AesWithHkdf Encrypt is the same as the result
// of the functions called within it
TEST(AesWithHkdfTest, compareEncrypt) {
  std::unique_ptr<Hkdf> hkdf = GetSha256Hkdf();
  std::unique_ptr<Aes> aes = GetAesSivCmac512();
  SecretData key = SecretDataFromStringView("key");
  std::string_view message = "Some data to encrypt.";
  auto result = hkdf->ComputeHkdf(key, aes->key_size_bytes());
  ASSERT_TRUE(result.ok()) << result.status();
  auto encrypted_other = aes->Encrypt(message, *result);
  AesWithHkdf aes_hkdf(std::move(hkdf), std::move(aes));
  auto encrypted_this = aes_hkdf.Encrypt(message, key);
  ASSERT_TRUE(encrypted_this.ok()) << encrypted_this.status();
  EXPECT_EQ(*encrypted_this, *encrypted_other);
}

// Tests that the result of AesWithHkdf Decrypt is the same as the result
// of the functions called within it
TEST(AesWithHkdfTest, compareDecrypt) {
  std::unique_ptr<Hkdf> hkdf = GetSha256Hkdf();
  std::unique_ptr<Aes> aes = GetAesSivCmac512();
  SecretData key = SecretDataFromStringView("key");
  std::string_view message = "Some data to encrypt.";
  auto result = hkdf->ComputeHkdf(key, aes->key_size_bytes());
  ASSERT_TRUE(result.ok()) << result.status();
  auto encrypted_other = aes->Encrypt(message, *result);
  auto decrypted_other = aes->Decrypt(*encrypted_other, *result);
  ASSERT_TRUE(decrypted_other.ok()) << decrypted_other.status();
  AesWithHkdf aes_hkdf(std::move(hkdf), std::move(aes));
  auto encrypted_this = aes_hkdf.Encrypt(message, key);
  ASSERT_TRUE(encrypted_this.ok()) << encrypted_this.status();
  auto decrypted_this = aes_hkdf.Decrypt(*encrypted_this, key);
  ASSERT_TRUE(decrypted_this.ok()) << decrypted_this.status();
  EXPECT_EQ(*decrypted_this, *decrypted_other);
}

// Test with empty key and proper input - Encrypt
TEST(AesWithHkdfTest, emptyKeyEncrypt) {
  std::unique_ptr<Hkdf> hkdf = GetSha256Hkdf();
  std::unique_ptr<Aes> aes = GetAesSivCmac512();
  AesWithHkdf aes_hkdf(std::move(hkdf), std::move(aes));
  auto result = aes_hkdf.Encrypt("input", SecretDataFromStringView(""));
  EXPECT_THAT(result.status(),
              StatusIs(absl::StatusCode::kInvalidArgument, ""));
}

// Test with empty key and proper input - Decrypt
TEST(AesWithHkdfTest, emptyKeyDecrypt) {
  std::unique_ptr<Hkdf> hkdf = GetSha256Hkdf();
  std::unique_ptr<Aes> aes = GetAesSivCmac512();
  AesWithHkdf aes_hkdf(std::move(hkdf), std::move(aes));
  auto result = aes_hkdf.Decrypt("input", SecretDataFromStringView(""));
  EXPECT_THAT(result.status(),
              StatusIs(absl::StatusCode::kInvalidArgument, ""));
}

// Tests that different keys with the same string return different values
TEST(AesTest, differentKeySameStringEncrypt) {
  std::unique_ptr<Hkdf> hkdf = GetSha256Hkdf();
  std::unique_ptr<Aes> aes = GetAesSivCmac512();
  SecretData key_1 = SecretDataFromStringView("key1");
  SecretData key_2 = SecretDataFromStringView("key2");
  std::string_view message = "Some data to encrypt.";
  AesWithHkdf aes_hkdf(std::move(hkdf), std::move(aes));
  auto encrypted_1 = aes_hkdf.Encrypt(message, key_1);
  auto encrypted_2 = aes_hkdf.Encrypt(message, key_2);
  ASSERT_TRUE(encrypted_1.ok()) << encrypted_1.status();
  ASSERT_TRUE(encrypted_2.ok()) << encrypted_2.status();
  EXPECT_NE(*encrypted_1, *encrypted_2);
}

// Tests that decrypting with a different key than encryption gives an error
TEST(AesTest, differentKeyDecrypt) {
  std::unique_ptr<Hkdf> hkdf = GetSha256Hkdf();
  std::unique_ptr<Aes> aes = GetAesSivCmac512();
  SecretData key_1 = SecretDataFromStringView("key1");
  SecretData key_2 = SecretDataFromStringView("key2");
  std::string_view message = "Some data to encrypt.";
  AesWithHkdf aes_hkdf(std::move(hkdf), std::move(aes));
  auto encrypted = aes_hkdf.Encrypt(message, key_1);
  ASSERT_TRUE(encrypted.ok()) << encrypted.status();
  auto decrypted = aes_hkdf.Decrypt(*encrypted, key_2);
  EXPECT_THAT(decrypted.status(),
              StatusIs(absl::StatusCode::kInvalidArgument, ""));
}

// Tests that the same key with different strings return different values
TEST(AesTest, sameKeyDiffernetStringEncrypt) {
  std::unique_ptr<Hkdf> hkdf = GetSha256Hkdf();
  std::unique_ptr<Aes> aes = GetAesSivCmac512();
  SecretData key = SecretDataFromStringView("key");
  std::string_view message_1 = "Some data to encrypt.";
  std::string_view message_2 = "Additional data to encrypt.";
  AesWithHkdf aes_hkdf(std::move(hkdf), std::move(aes));
  auto encrypted_1 = aes_hkdf.Encrypt(message_1, key);
  auto encrypted_2 = aes_hkdf.Encrypt(message_2, key);
  EXPECT_TRUE(encrypted_1.ok()) << encrypted_1.status();
  EXPECT_TRUE(encrypted_2.ok()) << encrypted_2.status();
  EXPECT_NE(*encrypted_1, *encrypted_2);
}

// Tests that the same key with different strings return different values
TEST(AesTest, sameKeyDiffernetStringDecrypt) {
  std::unique_ptr<Hkdf> hkdf = GetSha256Hkdf();
  std::unique_ptr<Aes> aes = GetAesSivCmac512();
  SecretData key = SecretDataFromStringView("key");
  std::string_view message_1 = "Some data to encrypt.";
  std::string_view message_2 = "Additional data to encrypt.";
  AesWithHkdf aes_hkdf(std::move(hkdf), std::move(aes));
  auto encrypted_1 = aes_hkdf.Encrypt(message_1, key);
  auto encrypted_2 = aes_hkdf.Encrypt(message_2, key);
  ASSERT_TRUE(encrypted_1.ok()) << encrypted_1.status();
  ASSERT_TRUE(encrypted_2.ok()) << encrypted_2.status();
  auto decrypted_1 = aes_hkdf.Decrypt(*encrypted_1, key);
  auto decrypted_2 = aes_hkdf.Decrypt(*encrypted_2, key);
  ASSERT_TRUE(decrypted_1.ok()) << decrypted_1.status();
  ASSERT_TRUE(decrypted_2.ok()) << decrypted_2.status();
  EXPECT_NE(*decrypted_1, *decrypted_2);
}

}  // namespace
}  // namespace wfanet::panelmatch::common::crypto
