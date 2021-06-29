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

#include <string>

#include "absl/strings/escaping.h"
#include "gtest/gtest.h"
#include "src/test/cc/testutil/matchers.h"
#include "tink/subtle/aes_siv_boringssl.h"
#include "tink/util/secret_data.h"

namespace wfanet::panelmatch::common::crypto {
namespace {

using ::crypto::tink::subtle::AesSivBoringSsl;
using ::crypto::tink::util::SecretData;
using ::crypto::tink::util::SecretDataFromStringView;

// Tests that a value encrypted then decrypted returns that original value
// Key values for these tests are found at
// tink/cc/subtle/aes_siv_boringssl_test.cc
TEST(AesTest, testEncryptDecrypt) {
  std::unique_ptr<Aes> aes = GetAesSivCmac512();
  SecretData key = SecretDataFromStringView(absl::HexStringToBytes(
      "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"
      "00112233445566778899aabbccddeefff0f1f2f3f4f5f6f7f8f9fafbfcfdfeff"));
  std::string_view message = "Some data to encrypt.";
  auto encrypted = aes->Encrypt(message, key);
  ASSERT_TRUE(encrypted.ok()) << encrypted.status();
  auto reverted = aes->Decrypt(*encrypted, key);
  ASSERT_TRUE(reverted.ok()) << reverted.status();
  EXPECT_EQ(*reverted, message);
}

// Tests that AesSiv Encrypt returns the same value as AesSivBoringSsl
// EncryptDeterministically
TEST(AesTest, compareEncrypt) {
  std::unique_ptr<Aes> aes_this = GetAesSivCmac512();
  SecretData key = SecretDataFromStringView(absl::HexStringToBytes(
      "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"
      "00112233445566778899aabbccddeefff0f1f2f3f4f5f6f7f8f9fafbfcfdfeff"));
  auto aes_other = AesSivBoringSsl::New(key);
  ASSERT_TRUE(aes_other.ok()) << aes_other.status();
  std::string message = "Some data to encrypt.";
  auto result_this = aes_this->Encrypt(message, key);
  auto result_other = (*aes_other)->EncryptDeterministically(message, "");
  ASSERT_TRUE(result_other.ok()) << result_other.status();
  EXPECT_THAT(result_this, result_other);
}

// Tests that AesSiv Decrypt returns the same value as AesSivBoringSsl
// DecryptDeterministically
TEST(AesTest, compareDecrypt) {
  std::unique_ptr<Aes> aes_this = GetAesSivCmac512();
  SecretData key = SecretDataFromStringView(absl::HexStringToBytes(
      "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"
      "00112233445566778899aabbccddeefff0f1f2f3f4f5f6f7f8f9fafbfcfdfeff"));
  auto aes_other = AesSivBoringSsl::New(key);
  ASSERT_TRUE(aes_other.ok()) << aes_other.status();
  std::string message = "Some data to encrypt.";
  auto encrypted = aes_this->Encrypt(message, key);
  auto result_this = aes_this->Decrypt(*encrypted, key);
  auto result_other = (*aes_other)->DecryptDeterministically(*encrypted, "");
  ASSERT_TRUE(result_other.ok()) << result_other.status();
  EXPECT_THAT(*result_this, *result_other);
}

// Tests that AesSiv Encrypt returns an error with the wrong key size
TEST(AesTest, wrongKeySizeEncrypt) {
  std::unique_ptr<Aes> aes = GetAesSivCmac512();
  SecretData key = SecretDataFromStringView(absl::HexStringToBytes("01"));
  auto result = aes->Encrypt("input", key);
  EXPECT_THAT(result.status(),
              StatusIs(absl::StatusCode::kInvalidArgument, ""));
}

// Tests that AesSiv Decrypt returns an error with the wrong key size
TEST(AesTest, wrongKeySizeDecrypt) {
  std::unique_ptr<Aes> aes = GetAesSivCmac512();
  SecretData key = SecretDataFromStringView(absl::HexStringToBytes(
      "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"
      "00112233445566778899aabbccddeefff0f1f2f3f4f5f6f7f8f9fafbfcfdfeff"));
  std::string message = "Some data to encrypt.";
  auto encrypted = aes->Encrypt(message, key);
  key = ::crypto::tink::util::SecretDataFromStringView(
      absl::HexStringToBytes("01"));
  auto result = aes->Decrypt(*encrypted, key);
  EXPECT_THAT(result.status(),
              StatusIs(absl::StatusCode::kInvalidArgument, ""));
}

// Tests that AesSiv Encrypt returns an error with an empty key
TEST(AesTest, emptyKeyEncrypt) {
  std::unique_ptr<Aes> aes = GetAesSivCmac512();
  SecretData key = SecretDataFromStringView(absl::HexStringToBytes(""));
  auto result = aes->Encrypt("input", key);
  EXPECT_THAT(result.status(),
              StatusIs(absl::StatusCode::kInvalidArgument, ""));
}

// Tests that AesSiv Decrypt returns an error with an empty key
TEST(AesTest, emptyKeyDecrypt) {
  std::unique_ptr<Aes> aes = GetAesSivCmac512();
  SecretData key = SecretDataFromStringView(absl::HexStringToBytes(
      "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"
      "00112233445566778899aabbccddeefff0f1f2f3f4f5f6f7f8f9fafbfcfdfeff"));
  std::string message = "Some data to encrypt.";
  auto encrypted = aes->Encrypt(message, key);
  key = ::crypto::tink::util::SecretDataFromStringView(
      absl::HexStringToBytes(""));
  auto result = aes->Decrypt(*encrypted, key);
  EXPECT_THAT(result.status(),
              StatusIs(absl::StatusCode::kInvalidArgument, ""));
}

// Tests that different keys with the same string return different values
TEST(AesTest, differentKeySameStringEncrypt) {
  std::unique_ptr<Aes> aes = GetAesSivCmac512();
  SecretData key_1 = SecretDataFromStringView(absl::HexStringToBytes(
      "990102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"
      "99112233445566778899aabbccddeefff0f1f2f3f4f5f6f7f8f9fafbfcfdfeff"));
  SecretData key_2 = SecretDataFromStringView(absl::HexStringToBytes(
      "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"
      "00112233445566778899aabbccddeefff0f1f2f3f4f5f6f7f8f9fafbfcfdfeff"));
  std::string_view message = "Some data to encrypt.";
  auto encrypted_1 = aes->Encrypt(message, key_1);
  auto encrypted_2 = aes->Encrypt(message, key_2);
  ASSERT_TRUE(encrypted_1.ok()) << encrypted_1.status();
  ASSERT_TRUE(encrypted_2.ok()) << encrypted_2.status();
  EXPECT_NE(*encrypted_1, *encrypted_2);
}

// Tests that decrypting with a different key than encryption gives an error
TEST(AesTest, differentKeyDecrypt) {
  std::unique_ptr<Aes> aes = GetAesSivCmac512();
  SecretData key_1 = SecretDataFromStringView(absl::HexStringToBytes(
      "990102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"
      "99112233445566778899aabbccddeefff0f1f2f3f4f5f6f7f8f9fafbfcfdfeff"));
  SecretData key_2 = SecretDataFromStringView(absl::HexStringToBytes(
      "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"
      "00112233445566778899aabbccddeefff0f1f2f3f4f5f6f7f8f9fafbfcfdfeff"));
  std::string_view message = "Some data to encrypt.";
  auto encrypted = aes->Encrypt(message, key_1);
  ASSERT_TRUE(encrypted.ok()) << encrypted.status();
  auto decrypted = aes->Decrypt(*encrypted, key_2);
  EXPECT_THAT(decrypted.status(),
              StatusIs(absl::StatusCode::kInvalidArgument, ""));
}

// Tests that the same key with different strings return different values
TEST(AesTest, sameKeyDiffernetStringEncrypt) {
  std::unique_ptr<Aes> aes = GetAesSivCmac512();
  SecretData key = SecretDataFromStringView(absl::HexStringToBytes(
      "990102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"
      "99112233445566778899aabbccddeefff0f1f2f3f4f5f6f7f8f9fafbfcfdfeff"));
  std::string_view message_1 = "Some data to encrypt.";
  std::string_view message_2 = "Additional data to encrypt.";
  auto encrypted_1 = aes->Encrypt(message_1, key);
  auto encrypted_2 = aes->Encrypt(message_2, key);
  EXPECT_TRUE(encrypted_1.ok()) << encrypted_1.status();
  EXPECT_TRUE(encrypted_2.ok()) << encrypted_2.status();
  EXPECT_NE(*encrypted_1, *encrypted_2);
}

// Tests that the same key with different strings return different values
TEST(AesTest, sameKeyDiffernetStringDecrypt) {
  std::unique_ptr<Aes> aes = GetAesSivCmac512();
  SecretData key = SecretDataFromStringView(absl::HexStringToBytes(
      "990102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"
      "99112233445566778899aabbccddeefff0f1f2f3f4f5f6f7f8f9fafbfcfdfeff"));
  std::string_view message_1 = "Some data to encrypt.";
  std::string_view message_2 = "Additional data to encrypt.";
  auto encrypted_1 = aes->Encrypt(message_1, key);
  auto encrypted_2 = aes->Encrypt(message_2, key);
  ASSERT_TRUE(encrypted_1.ok()) << encrypted_1.status();
  ASSERT_TRUE(encrypted_2.ok()) << encrypted_2.status();
  auto decrypted_1 = aes->Decrypt(*encrypted_1, key);
  auto decrypted_2 = aes->Decrypt(*encrypted_2, key);
  ASSERT_TRUE(decrypted_1.ok()) << decrypted_1.status();
  ASSERT_TRUE(decrypted_2.ok()) << decrypted_2.status();
  EXPECT_NE(*decrypted_1, *decrypted_2);
}

}  // namespace
}  // namespace wfanet::panelmatch::common::crypto
