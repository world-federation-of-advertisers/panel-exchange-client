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
#include "wfanet/panelmatch/common/crypto/cryptor.h"
#include "wfanet/panelmatch/common/crypto/encryption_utility_helper.h"
#include "wfanet/panelmatch/common/macros.h"
#include "wfanet/panelmatch/protocol/crypto/cryptor.pb.h"

namespace wfanet::panelmatch::protocol::crypto {

namespace {

using ::wfanet::panelmatch::common::crypto::Action;
using ::wfanet::panelmatch::common::crypto::CreateCryptorFromKey;

}  // namespace

absl::StatusOr<::wfanet::panelmatch::protocol::protobuf::
                   ApplyCommutativeEncryptionResponse>
ApplyCommutativeEncryption(const ::wfanet::panelmatch::protocol::protobuf::
                               ApplyCommutativeEncryptionRequest& request) {
  ::wfa::measurement::common::crypto::StartedThreadCpuTimer timer;
  ::wfanet::panelmatch::protocol::protobuf::ApplyCommutativeEncryptionResponse
      response;
  ASSIGN_OR_RETURN_ERROR(auto cryptor,
                         CreateCryptorFromKey(request.encryption_key()),
                         "Failed to create the protocol cipher");
  ASSIGN_OR_RETURN(
      auto encrypted_texts,
      cryptor->BatchProcess(request.plaintexts(), Action::kEncrypt));
  response.mutable_encrypted_texts()->Swap(&encrypted_texts);
  response.set_elapsed_cpu_time_millis(timer.ElapsedMillis());
  return response;
}

absl::StatusOr<::wfanet::panelmatch::protocol::protobuf::
                   ApplyCommutativeDecryptionResponse>
ApplyCommutativeDecryption(const ::wfanet::panelmatch::protocol::protobuf::
                               ApplyCommutativeDecryptionRequest& request) {
  ::wfa::measurement::common::crypto::StartedThreadCpuTimer timer;
  ::wfanet::panelmatch::protocol::protobuf::ApplyCommutativeDecryptionResponse
      response;
  ASSIGN_OR_RETURN_ERROR(auto cryptor,
                         CreateCryptorFromKey(request.encryption_key()),
                         "Failed to create the protocol cipher");
  ASSIGN_OR_RETURN(
      auto decrypted_texts,
      cryptor->BatchProcess(request.encrypted_texts(), Action::kDecrypt));
  response.mutable_decrypted_texts()->Swap(&decrypted_texts);
  response.set_elapsed_cpu_time_millis(timer.ElapsedMillis());
  return response;
}

absl::StatusOr<wfanet::panelmatch::protocol::protobuf::
                   ReApplyCommutativeEncryptionResponse>
ReApplyCommutativeEncryption(const wfanet::panelmatch::protocol::protobuf::
                                 ReApplyCommutativeEncryptionRequest& request) {
  ::wfa::measurement::common::crypto::StartedThreadCpuTimer timer;
  ::wfanet::panelmatch::protocol::protobuf::ReApplyCommutativeEncryptionResponse
      response;
  ASSIGN_OR_RETURN_ERROR(auto cryptor,
                         CreateCryptorFromKey(request.encryption_key()),
                         "Failed to create the protocol cipher");
  ASSIGN_OR_RETURN(
      auto reencrypted_texts,
      cryptor->BatchProcess(request.encrypted_texts(), Action::kReEncrypt));
  response.mutable_reencrypted_texts()->Swap(&reencrypted_texts);
  response.set_elapsed_cpu_time_millis(timer.ElapsedMillis());
  return response;
}

}  // namespace wfanet::panelmatch::protocol::crypto
