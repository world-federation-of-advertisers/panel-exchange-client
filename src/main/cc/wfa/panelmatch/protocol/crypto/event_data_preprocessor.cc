// Copyright 2021 The Cross-Media Measurement Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#include "wfa/panelmatch/protocol/crypto/event_data_preprocessor.h"

#include <memory>
#include <utility>
#include <vector>

#include "absl/status/status.h"
#include "absl/status/statusor.h"
#include "absl/strings/string_view.h"
#include "common_cpp/fingerprinters/fingerprinters.h"
#include "common_cpp/macros/macros.h"
#include "glog/logging.h"
#include "tink/util/secret_data.h"
#include "wfa/panelmatch/common/crypto/aes.h"
#include "wfa/panelmatch/common/crypto/aes_with_hkdf.h"
#include "wfa/panelmatch/common/crypto/cryptor.h"
#include "wfa/panelmatch/common/crypto/hkdf.h"
#include "wfa/panelmatch/common/crypto/peppered_fingerprinter.h"

namespace wfa::panelmatch::protocol::crypto {

using ::crypto::tink::util::SecretData;
using ::crypto::tink::util::SecretDataAsStringView;
using ::crypto::tink::util::SecretDataFromStringView;
using ::wfa::panelmatch::common::crypto::Action;
using ::wfa::panelmatch::common::crypto::AesWithHkdf;
using ::wfa::panelmatch::common::crypto::CreateCryptorFromKey;
using ::wfa::panelmatch::common::crypto::Cryptor;
using ::wfa::panelmatch::common::crypto::GetPepperedFingerprinter;

EventDataPreprocessor::EventDataPreprocessor(std::unique_ptr<Cryptor> cryptor,
                                             const SecretData& pepper,
                                             const wfa::Fingerprinter* delegate,
                                             const AesWithHkdf& aes_hkdf)
    : cryptor_(std::move(cryptor)),
      pepper_(pepper),
      delegate_(CHECK_NOTNULL(delegate)),
      aes_hkdf_(aes_hkdf) {}

absl::StatusOr<ProcessedData> EventDataPreprocessor::Process(
    absl::string_view identifier, absl::string_view event_data) const {
  std::vector<std::string> input = {std::string(identifier)};
  ASSIGN_OR_RETURN(std::vector<std::string> processed,
                   cryptor_->BatchProcess(input, Action::kEncrypt));

  if (processed.size() != 1)
    return absl::InternalError("Incorrect vector size");

  ProcessedData processed_data = ProcessedData();
  ASSIGN_OR_RETURN(
      processed_data.encrypted_event_data,
      aes_hkdf_.Encrypt(event_data, SecretDataFromStringView(processed[0])));

  std::unique_ptr<wfa::Fingerprinter> fingerprinter =
      GetPepperedFingerprinter(delegate_, pepper_);
  processed_data.encrypted_identifier =
      fingerprinter->Fingerprint(processed[0]);

  return processed_data;
}

}  // namespace wfa::panelmatch::protocol::crypto
