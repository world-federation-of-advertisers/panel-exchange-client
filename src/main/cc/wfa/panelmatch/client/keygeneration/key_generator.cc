#include "wfa/panelmatch/client/keygeneration/key_generator.h"

#include <memory>

#include "crypto/ec_commutative_cipher.h"
#include "openssl/rand.h"
#include "wfa/panelmatch/common/crypto/cryptor.h"

namespace wfa::panelmatch::client::keygeneration {

using ::crypto::tink::util::SecretData;
using ::crypto::tink::util::SecretDataFromStringView;
using ::private_join_and_compute::ECCommutativeCipher;

absl::StatusOr<GeneratedKeys> KeyGenerator::GenerateKeys() const {
  GeneratedKeys keys;
  uint8_t pepper[32];
  RAND_bytes(pepper, sizeof(keys.pepper));
  keys.pepper = SecretDataFromStringView((char*)pepper);
  ASSIGN_OR_RETURN(
      std::unique_ptr<ECCommutativeCipher> cipher,
      ECCommutativeCipher::CreateWithNewKey(
          NID_X9_62_prime256v1, ECCommutativeCipher::HashType::SHA256));
  keys.cryptokey = SecretDataFromStringView(cipher->GetPrivateKeyBytes());

  return keys;
}

}  // namespace wfa::panelmatch::client::keygeneration
