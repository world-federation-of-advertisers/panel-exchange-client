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

#include "wfanet/panelmatch/common/crypto/peppered_fingerprinter.h"

#include "gtest/gtest.h"
#include "src/main/cc/any_sketch/fingerprinters/fingerprinters.h"
#include "tink/util/secret_data.h"

namespace wfanet::panelmatch::common::crypto {
namespace {

using ::crypto::tink::util::SecretDataFromStringView;
using wfa::any_sketch::Fingerprinter;
using wfa::any_sketch::GetFarmFingerprinter;
using wfa::any_sketch::GetSha256Fingerprinter;

// Test PepperedFingerprinter with Sha256Fingerprinter delegate
// Test values from
// any-sketch/src/test/cc/any_sketch/fingerprinters/fingerprinters_test.cc
TEST(FingerprintersTest, PepperedFingerprinterWithSha256) {
  const Fingerprinter& sha = GetSha256Fingerprinter();
  std::unique_ptr<Fingerprinter> fingerprinter =
      GetPepperedFingerprinter(&sha, SecretDataFromStringView("-str"));
  EXPECT_NE(fingerprinter->Fingerprint("a-dfferent"), 0x141cfc9842c4b0e3);

  std::unique_ptr<Fingerprinter> empty_fingerprinter =
      GetPepperedFingerprinter(&sha, SecretDataFromStringView(""));
  EXPECT_EQ(empty_fingerprinter->Fingerprint(""), 0x141cfc9842c4b0e3);
}

// Test PepperedFingerprinter with FarmHashFingerprinter delegate
// Test values from
// any-sketch/src/test/cc/any_sketch/fingerprinters/fingerprinters_test.cc
TEST(FingerprintersTest, PepperedFingerprinterWithFarm) {
  const Fingerprinter& farm = GetFarmFingerprinter();
  std::unique_ptr<Fingerprinter> fingerprinter =
      GetPepperedFingerprinter(&farm, SecretDataFromStringView("-str"));
  EXPECT_NE(fingerprinter->Fingerprint("not-an-empty"), 0x9ae16a3b2f90404f);

  std::unique_ptr<Fingerprinter> empty_fingerprinter =
      GetPepperedFingerprinter(&farm, SecretDataFromStringView(""));
  EXPECT_EQ(empty_fingerprinter->Fingerprint(""), 0x9ae16a3b2f90404f);
}

// Test PepperedFingerprinter with null delegate
TEST(FingerprintersDeathTest, NullFingerprinter) {
  ASSERT_DEATH(
      GetPepperedFingerprinter(nullptr, SecretDataFromStringView("-str")), "");
}

}  // namespace
}  // namespace wfanet::panelmatch::common::crypto
