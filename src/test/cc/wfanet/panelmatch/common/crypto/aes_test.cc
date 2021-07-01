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

#include "gtest/gtest.h"
#include "src/test/cc/testutil/matchers.h"
// #include "tink/util/secret_data.h"

namespace wfanet::panelmatch::common::crypto {
namespace {

using ::crypto::tink::util::SecretDataFromStringView;

TEST(AesTest, unimplementedEncrypt) {
  const Aes& aes = GetAesSiv();
  auto result = aes.Encrypt("input", SecretDataFromStringView("key"));
  EXPECT_THAT(result.status(), StatusIs(absl::StatusCode::kUnimplemented, ""));
}

TEST(AesTest, unimplementedDecrypt) {
  const Aes& aes = GetAesSiv();
  auto result = aes.Decrypt("input", SecretDataFromStringView("key"));
  EXPECT_THAT(result.status(), StatusIs(absl::StatusCode::kUnimplemented, ""));
}

// A round trip test to ensure the value encrypted then decrypted results in the
// original value
// TEST(AesTest, EncryptDecrypt) {
// util::SecretData key = util::SecretDataFromStringView(test::HexDecodeOrDie(
//     "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"
//     "00112233445566778899aabbccddeefff0f1f2f3f4f5f6f7f8f9fafbfcfdfeff"));
// auto res = AesSivBoringSsl::New(key);
// EXPECT_TRUE(res.ok()) << res.status();
// auto cipher = std::move(res.ValueOrDie());
// std::string aad = "Additional data";
// std::string message = "Some data to encrypt.";
// auto ct = cipher->Encrypt(message, aad);
// EXPECT_TRUE(ct.ok()) << ct.status();
// auto pt = cipher->Decrypt(ct.ValueOrDie(), aad);
// EXPECT_TRUE(pt.ok()) << pt.status();
// EXPECT_EQ(pt.ValueOrDie(), message);
// }

// Compares the output of AesSiv to AesSivBoringSsl

// Wrong key size

// Null input

// Null key

// Different key with same string are different things

// Same key with different strings are different things

}  // namespace
}  // namespace wfanet::panelmatch::common::crypto
