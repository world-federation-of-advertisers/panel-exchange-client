#ifndef SRC_MAIN_CC_WFA_PANELMATCH_CLIENT_KEYGENERATION_KEY_GENERATOR_H_
#define SRC_MAIN_CC_WFA_PANELMATCH_CLIENT_KEYGENERATION_KEY_GENERATOR_H_

#include "absl/status/statusor.h"
#include "tink/util/secret_data.h"

namespace wfa::panelmatch::client::keygeneration {

struct GeneratedKeys {
  crypto::tink::util::SecretData pepper;
  crypto::tink::util::SecretData cryptokey;
};

class KeyGenerator {
 public:
  KeyGenerator() = default;
  ~KeyGenerator() = default;

  absl::StatusOr<GeneratedKeys> GenerateKeys() const;
};

}  // namespace wfa::panelmatch::client::keygeneration

#endif  // SRC_MAIN_CC_WFA_PANELMATCH_CLIENT_KEYGENERATION_KEY_GENERATOR_H_
