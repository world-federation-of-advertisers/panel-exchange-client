#include "wfa/panelmatch/common/crypto/key_loader.h"

#include "absl/status/statusor.h"
#include "absl/strings/string_view.h"
#include "tink/util/secret_data.h"

// Link this into a binary to register an IdentityKeyLoader: it will just use
// the key name as the key material itself.

namespace wfa::panelmatch::common::crypto {
namespace {
class IdentityKeyLoader : public KeyLoader {
  virtual absl::StatusOr<::crypto::tink::util::SecretData> LoadKey(
      absl::string_view key_name) {
    return ::crypto::tink::util::SecretDataFromStringView(key_name);
  }
};
}  // namespace

void RegisterIdentityKeyLoader() {
  static auto* key_loader = new IdentityKeyLoader;
  RegisterGlobalKeyLoader(key_loader);
}
}  // namespace wfa::panelmatch::common::crypto
