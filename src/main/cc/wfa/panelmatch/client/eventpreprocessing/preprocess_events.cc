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

#include "wfa/panelmatch/client/eventpreprocessing/preprocess_events.h"

#include <memory>
#include <string>
#include <utility>

#include "absl/algorithm/container.h"
#include "absl/memory/memory.h"
#include "absl/status/statusor.h"
#include "absl/strings/str_cat.h"
#include "absl/types/span.h"
#include "common_cpp/fingerprinters/fingerprinters.h"
#include "common_cpp/macros/macros.h"
#include "tink/util/secret_data.h"
#include "wfa/panelmatch/client/eventpreprocessing/preprocess_events.pb.h"
#include "wfa/panelmatch/common/crypto/aes.h"
#include "wfa/panelmatch/common/crypto/aes_with_hkdf.h"
#include "wfa/panelmatch/common/crypto/deterministic_commutative_cipher.h"
#include "wfa/panelmatch/common/crypto/hkdf.h"
#include "wfa/panelmatch/protocol/crypto/event_data_preprocessor.h"

namespace wfa::panelmatch::client {
using ::crypto::tink::util::SecretData;
using ::crypto::tink::util::SecretDataAsStringView;
using ::crypto::tink::util::SecretDataFromStringView;
using ::wfa::panelmatch::common::crypto::Aes;
using ::wfa::panelmatch::common::crypto::AesWithHkdf;
using ::wfa::panelmatch::common::crypto::DeterministicCommutativeCipher;
using ::wfa::panelmatch::common::crypto::GetAesSivCmac512;
using ::wfa::panelmatch::common::crypto::GetSha256Hkdf;
using ::wfa::panelmatch::common::crypto::Hkdf;
using ::wfa::panelmatch::common::crypto::NewDeterministicCommutativeCipher;
using ::wfa::panelmatch::protocol::crypto::EventDataPreprocessor;
using ::wfa::panelmatch::protocol::crypto::ProcessedData;

absl::StatusOr<PreprocessEventsResponse> PreprocessEvents(
    const PreprocessEventsRequest& request) {
  SecretData key = SecretDataFromStringView(request.crypto_key());
  ASSIGN_OR_RETURN(auto cipher, NewDeterministicCommutativeCipher(key));

  const Fingerprinter& fingerprinter = GetSha256Fingerprinter();

  std::unique_ptr<Aes> aes = GetAesSivCmac512();
  std::unique_ptr<Hkdf> hkdf = GetSha256Hkdf();
  const AesWithHkdf aes_hkdf = AesWithHkdf(std::move(hkdf), std::move(aes));

  if (request.identifier_hash_pepper().empty()) {
    return absl::InvalidArgumentError("Empty Identifier Hash Pepper");
  }
  if (request.hkdf_pepper().empty()) {
    return absl::InvalidArgumentError("Empty HKDF Pepper");
  }
  EventDataPreprocessor preprocessor(
      std::move(cipher),
      SecretDataFromStringView(request.identifier_hash_pepper()),
      SecretDataFromStringView(request.hkdf_pepper()), &fingerprinter,
      &aes_hkdf);
  PreprocessEventsResponse processed;
  for (const PreprocessEventsRequest::UnprocessedEvent& u :
       request.unprocessed_events()) {
    ASSIGN_OR_RETURN(ProcessedData data,
                     preprocessor.Process(u.id(), u.data()));
    PreprocessEventsResponse::ProcessedEvent* processed_event =
        processed.add_processed_events();
    processed_event->set_encrypted_data(data.encrypted_event_data);
    processed_event->set_encrypted_id(data.encrypted_identifier);
  }
  return processed;
}
}  // namespace wfa::panelmatch::client
