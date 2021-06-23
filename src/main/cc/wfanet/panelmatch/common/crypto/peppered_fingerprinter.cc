#include "wfanet/panelmatch/common/crypto/peppered_fingerprinter.h"

#include "absl/memory/memory.h"
#include "absl/status/status.h"
#include "glog/logging.h"
#include "src/main/cc/any_sketch/fingerprinters/fingerprinters.h"
#include "tink/util/secret_data.h"

namespace wfanet::panelmatch::common::crypto {

using ::crypto::tink::util::SecretData;
using wfa::any_sketch::Fingerprinter;

namespace {

using ::crypto::tink::util::SecretDataAsStringView;

// A hashfunction that concatenates a pepper to an input, then uses
// a specified hashfunction.
class PepperedFingerprinter : public Fingerprinter {
 public:
  ~PepperedFingerprinter() override = default;

  PepperedFingerprinter(const Fingerprinter* delegate, const SecretData& pepper)
      : pepper_(pepper), delegate_(CHECK_NOTNULL(delegate)) {}

  // Uses 'delegate' to hash the concatenation of 'pepper' and 'item'
  uint64_t Fingerprint(absl::Span<const unsigned char> item) const override {
    absl::string_view item_as_string_view(
        reinterpret_cast<const char*>(item.data()), item.size());
    return delegate_->Fingerprint(
        absl::StrCat(item_as_string_view, SecretDataAsStringView(pepper_)));
  }

 private:
  const SecretData& pepper_;
  const Fingerprinter* delegate_;
};
}  // namespace

std::unique_ptr<Fingerprinter> GetPepperedFingerprinter(
    const Fingerprinter* delegate, SecretData pepper) {
  return absl::make_unique<PepperedFingerprinter>(delegate, pepper);
}
}  // namespace wfanet::panelmatch::common::crypto
