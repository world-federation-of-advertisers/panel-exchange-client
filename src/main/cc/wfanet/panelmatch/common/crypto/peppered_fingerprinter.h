#ifndef SRC_MAIN_CC_WFANET_PANELMATCH_COMMON_CRYPTO_PEPPERED_FINGERPRINTER_H_
#define SRC_MAIN_CC_WFANET_PANELMATCH_COMMON_CRYPTO_PEPPERED_FINGERPRINTER_H_

#include "src/main/cc/any_sketch/fingerprinters/fingerprinters.h"
#include "tink/util/secret_data.h"

namespace wfanet::panelmatch::common::crypto {

using ::crypto::tink::util::SecretData;
using wfa::any_sketch::Fingerprinter;

// Returns a Fingerprinter with a hashfunction 'delegate' and pepper 'pepper'
std::unique_ptr<Fingerprinter> GetPepperedFingerprinter(
    const Fingerprinter* delegate, SecretData pepper);

}  // namespace wfanet::panelmatch::common::crypto

#endif  // WFANET_PANELMATCH_COMMON_CRYPTO_PEPPERED_FINGERPRINTER_H_
