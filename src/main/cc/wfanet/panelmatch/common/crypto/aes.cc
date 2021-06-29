#include "wfanet/panelmatch/common/crypto/aes.h"
//#include "tink/cc/subtle/aes_siv_boringssl.h"
#include "absl/status/status.h"

namespace wfanet::panelmatch::common::crypto {
namespace {

using absl::string_view;
using ::crypto::tink::util::SecretData;

class AesSiv : public Aes {
 public:
  AesSiv() = default;

  absl::StatusOr<std::string> Encrypt(string_view input,
                                      const SecretData& key) const override {
    //    AesSivBoringSsl implementation = new AesSivBoringSsl(key);
    //    return implementation.EncryptDeterministically(input, "");

    return absl::UnimplementedError("Not implemented");
  }

  absl::StatusOr<std::string> Decrypt(string_view input,
                                      const SecretData& key) const override {
    //    AesSivBoringSsl implementation = new AesSivBoringSsl(key);
    //    return implementation.DecryptDeterministically(input, "");

    return absl::UnimplementedError("Not implemented");
  }
};
}  // namespace

const Aes& GetAesSiv() {
  static const auto* const aes = new AesSiv();
  return *aes;
}
}  // namespace wfanet::panelmatch::common::crypto
