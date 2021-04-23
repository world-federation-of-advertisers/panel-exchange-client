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

//using ::wfanet::panelmatch::common::crypto::CreateCipher;
using ::private_join_and_compute::ECCommutativeCipher;

namespace wfa::panelmatch {
namespace {
TEST(PrivateJoinAndComputeTest, EncryptReEncrypt) {
  std::unique_ptr<Cryptor> cryptor1 = std::move(CreateCryptorFromKey("9ias9fi0s").value());
  std::unique_ptr<Cryptor> cryptor2 = std::move(CreateCryptorFromKey("asdfasdfs").value());

  absl::StatusOr<std::string> encrypted_value1 = cryptor1->Encrypt("some plaintext1");
  ASSERT_THAT(encrypted_value1, IsOk());

  absl::StatusOr<std::string> encrypted_value2 = cryptor2->Encrypt("some plaintext1");
  ASSERT_THAT(encrypted_value2, IsOk());

  absl::StatusOr<std::string> double_encrypted_value1 = cryptor2->ReEncrypt(*encrypted_value1);
  ASSERT_THAT(double_encrypted_value1, IsOk());

  absl::StatusOr<std::string> double_encrypted_value2 = cryptor1->ReEncrypt(*encrypted_value2);
  ASSERT_THAT(double_encrypted_value2, IsOkAndHolds(*double_encrypted_value1));

  EXPECT_THAT(cryptor1->Decrypt(*double_encrypted_value1), IsOkAndHolds(*encrypted_value2));
  EXPECT_THAT(cryptor1->Decrypt(*double_encrypted_value2), IsOkAndHolds(*encrypted_value2));

  EXPECT_THAT(cryptor2->Decrypt(*double_encrypted_value1), IsOkAndHolds(*encrypted_value1));
  EXPECT_THAT(cryptor2->Decrypt(*double_encrypted_value2), IsOkAndHolds(*encrypted_value1));

}
}  // namespace
}  // namespace wfa::panelmatch
