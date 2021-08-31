#include "wfa/panelmatch/common/crypto/identity_key_loader.h"

namespace wfa::panelmatch::common::crypto {
namespace {
auto registered = (RegisterIdentityKeyLoader(), true);
}
}  // namespace wfa::panelmatch::common::crypto
