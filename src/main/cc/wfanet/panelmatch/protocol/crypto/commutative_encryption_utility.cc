// Copyright 2020 The Cross-Media Measurement Authors
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

#include "wfanet/panelmatch/protocol/crypto/commutative_encryption_utility.h"

#include <wfa/measurement/common/crypto/started_thread_cpu_timer.h>

#include <algorithm>
#include <string>
#include <utility>
#include <vector>

#include "absl/algorithm/container.h"
#include "absl/memory/memory.h"
#include "absl/status/statusor.h"
#include "absl/strings/str_cat.h"
#include "absl/types/span.h"
#include "util/status_macros.h"
#include "wfa/panelmatch/crypto/cryptor.pb.h"
#include "wfanet/panelmatch/common/crypto/cryptor.h"
#include "wfanet/panelmatch/common/crypto/encryption_utility_helper.h"
#include "wfanet/panelmatch/common/macros.h"

namespace wfanet::panelmatch::protocol::crypto {
using ::wfa::panelmatch::crypto::ApplyCommutativeDecryptionRequest;
using ::wfa::panelmatch::crypto::ApplyCommutativeDecryptionResponse;
using ::wfa::panelmatch::crypto::ApplyCommutativeEncryptionRequest;
using ::wfa::panelmatch::crypto::ApplyCommutativeEncryptionResponse;
using ::wfa::panelmatch::crypto::ReApplyCommutativeEncryptionRequest;
using ::wfa::panelmatch::crypto::ReApplyCommutativeEncryptionResponse;
using ::wfanet::panelmatch::common::crypto::Action;
using ::wfanet::panelmatch::common::crypto::CreateCryptorFromKey;

absl::StatusOr<ApplyCommutativeEncryptionResponse> ApplyCommutativeEncryption(
    const ApplyCommutativeEncryptionRequest& request) {
  wfa::measurement::common::crypto::StartedThreadCpuTimer timer;
  ApplyCommutativeEncryptionResponse response;
  ASSIGN_OR_RETURN_ERROR(auto cryptor,
                         CreateCryptorFromKey(request.encryption_key()),
                         "Failed to create the protocol cipher");
  ASSIGN_OR_RETURN(
      *response.mutable_encrypted_texts(),
      cryptor->BatchProcess(request.plaintexts(), Action::kEncrypt));
  response.set_elapsed_cpu_time_millis(timer.ElapsedMillis());
  return response;
}

absl::StatusOr<ApplyCommutativeDecryptionResponse> ApplyCommutativeDecryption(
    const ApplyCommutativeDecryptionRequest& request) {
  wfa::measurement::common::crypto::StartedThreadCpuTimer timer;
  ApplyCommutativeDecryptionResponse response;
  ASSIGN_OR_RETURN_ERROR(auto cryptor,
                         CreateCryptorFromKey(request.encryption_key()),
                         "Failed to create the protocol cipher");
  ASSIGN_OR_RETURN(
      *response.mutable_decrypted_texts(),
      cryptor->BatchProcess(request.encrypted_texts(), Action::kDecrypt));
  response.set_elapsed_cpu_time_millis(timer.ElapsedMillis());
  return response;
}

absl::StatusOr<ReApplyCommutativeEncryptionResponse>
ReApplyCommutativeEncryption(
    const ReApplyCommutativeEncryptionRequest& request) {
  wfa::measurement::common::crypto::StartedThreadCpuTimer timer;
  ReApplyCommutativeEncryptionResponse response;
  ASSIGN_OR_RETURN_ERROR(auto cryptor,
                         CreateCryptorFromKey(request.encryption_key()),
                         "Failed to create the protocol cipher");
  ASSIGN_OR_RETURN(
      *response.mutable_reencrypted_texts(),
      cryptor->BatchProcess(request.encrypted_texts(), Action::kReEncrypt));
  response.set_elapsed_cpu_time_millis(timer.ElapsedMillis());
  return response;
}

}  // namespace wfanet::panelmatch::protocol::crypto
