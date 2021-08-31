#include "wfa/panelmatch/common/crypto/key_loader.h"

#include "absl/status/statusor.h"
#include "absl/status/status.h"
#include "absl/strings/string_view.h"
#include "common_cpp/testing/status_macros.h"
#include "gtest/gtest.h"
#include "tink/util/secret_data.h"

namespace wfa::panelmatch::common::crypto {
namespace {
using ::crypto::tink::util::SecretData;
using ::crypto::tink::util::SecretDataAsStringView;

TEST(IdentityKeyLoaderTest, RegistersAnIdentityKeyLoader) {
  ASSERT_OK_AND_ASSIGN(SecretData key_material, LoadKey("abc"));
  EXPECT_EQ(SecretDataAsStringView(key_material), "abc");
}
}  // namespace
}  // namespace wfa::panelmatch::common::crypto
