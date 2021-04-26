#include <string>

#include "absl/memory/memory.h"
#include "absl/base/port.h"
#include "absl/status/status.h"
#include "absl/status/statusor.h"

#include "absl/memory/memory.h"
#include "absl/types/span.h"
#include "crypto/ec_commutative_cipher.h"
#include "wfanet/panelmatch/common/crypto/cryptor.h"

#include "gtest/gtest.h"
#include "src/test/cc/testutil/matchers.h"

using ::wfanet::IsOkAndHolds;
using ::wfanet::IsOk;
using ::wfanet::panelmatch::common::crypto::Cryptor;
using ::wfanet::panelmatch::common::crypto::CreateCryptorWithNewKey;
using ::wfanet::panelmatch::common::crypto::CreateCryptorFromKey;
using ::wfanet::panelmatch::common::crypto::Action;

//using ::wfanet::panelmatch::common::crypto::CreateCipher;
using ::private_join_and_compute::ECCommutativeCipher;

namespace wfa::panelmatch {
namespace {
  TEST(PrivateJoinAndComputeTest, EncryptReEncryptDecrypt) {
    std::unique_ptr<Cryptor> cryptor1 = std::move(CreateCryptorFromKey("9ias9fi0s").value());
    std::unique_ptr<Cryptor> cryptor2 = std::move(CreateCryptorFromKey("asdfasdfs").value());

    std::string plaintext = "some plaintext";
    absl::StatusOr<std::string> encrypted_value1 = cryptor1->Encrypt(plaintext);
    ASSERT_THAT(encrypted_value1, IsOk());

    absl::StatusOr<std::string> encrypted_value2 = cryptor2->Encrypt(plaintext);
    ASSERT_THAT(encrypted_value2, IsOk());

    absl::StatusOr<std::string> double_encrypted_value1 = cryptor2->ReEncrypt(*encrypted_value1);
    ASSERT_THAT(double_encrypted_value1, IsOk());

    absl::StatusOr<std::string> double_encrypted_value2 = cryptor1->ReEncrypt(*encrypted_value2);
    ASSERT_THAT(double_encrypted_value2, IsOkAndHolds(*double_encrypted_value1));

    EXPECT_THAT(cryptor1->Decrypt(*double_encrypted_value1), IsOkAndHolds(*encrypted_value2));
    EXPECT_THAT(cryptor1->Decrypt(*double_encrypted_value2), IsOkAndHolds(*encrypted_value2));

    EXPECT_THAT(cryptor2->Decrypt(*double_encrypted_value1), IsOkAndHolds(*encrypted_value1));
    EXPECT_THAT(cryptor2->Decrypt(*double_encrypted_value2), IsOkAndHolds(*encrypted_value1));

    std::string plaintext0 = "some plaintext0";
    std::string plaintext1 = "some plaintext1";
    std::string plaintext2 = "some plaintext2";
    std::string plaintext3 = "some plaintext3";
    std::string plaintext4 = "some plaintext4";
    std::vector <std::string> plaintext_v {
          plaintext0,
          plaintext1,
          plaintext2,
          plaintext3,
          plaintext4
    };
    absl::StatusOr<std::vector <std::string>> encrypted_vector1 = cryptor1->BatchProcess(plaintext_v, Action::Encrypt);
    ASSERT_THAT(encrypted_vector1, IsOk());
    absl::StatusOr<std::vector <std::string>> encrypted_vector2 = cryptor2->BatchProcess(plaintext_v, Action::Encrypt);
    ASSERT_THAT(encrypted_vector2, IsOk());

    absl::StatusOr<std::vector <std::string>> double_encrypted_vector1 = cryptor1->BatchProcess(*encrypted_vector2, Action::ReEncrypt);
    ASSERT_THAT(double_encrypted_vector1, IsOk());
    absl::StatusOr<std::vector <std::string>> double_encrypted_vector2 = cryptor2->BatchProcess(*encrypted_vector1, Action::ReEncrypt);
    ASSERT_THAT(double_encrypted_vector2, IsOk());

    EXPECT_THAT(cryptor1->BatchProcess(*double_encrypted_vector1, Action::Decrypt), IsOkAndHolds(*encrypted_vector2));
    EXPECT_THAT(cryptor1->BatchProcess(*double_encrypted_vector2, Action::Decrypt), IsOkAndHolds(*encrypted_vector2));

    EXPECT_THAT(cryptor2->BatchProcess(*double_encrypted_vector1, Action::Decrypt), IsOkAndHolds(*encrypted_vector1));
    EXPECT_THAT(cryptor2->BatchProcess(*double_encrypted_vector2, Action::Decrypt), IsOkAndHolds(*encrypted_vector1));
  }
}  // namespace
}  // namespace wfa::panelmatch
