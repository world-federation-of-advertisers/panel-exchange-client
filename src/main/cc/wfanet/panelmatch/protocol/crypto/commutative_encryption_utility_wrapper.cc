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

#include "wfanet/panelmatch/protocol/crypto/commutative_encryption_utility_wrapper.h"

#include "absl/memory/memory.h"
#include "absl/status/statusor.h"
#include "util/status_macros.h"
#include "wfanet/panelmatch/common/crypto/encryption_utility_helper.h"
#include "wfanet/panelmatch/protocol/crypto/commutative_encryption_utility.h"
#include "wfanet/panelmatch/protocol/crypto/cryptor.pb.h"

namespace wfanet::panelmatch::protocol::crypto {

using ::wfanet::panelmatch::common::crypto::ParseRequestFromString;

using ::wfanet::panelmatch::protocol::crypto::ApplyCommutativeDecryption;
using ::wfanet::panelmatch::protocol::crypto::ApplyCommutativeEncryption;
using ::wfanet::panelmatch::protocol::crypto::ReApplyCommutativeEncryption;

using ::wfanet::panelmatch::protocol::crypto::ApplyCommutativeDecryptionRequest;
using ::wfanet::panelmatch::protocol::crypto::
    ApplyCommutativeDecryptionResponse;
using ::wfanet::panelmatch::protocol::crypto::ApplyCommutativeEncryptionRequest;
using ::wfanet::panelmatch::protocol::crypto::
    ApplyCommutativeEncryptionResponse;
using ::wfanet::panelmatch::protocol::crypto::
    ReApplyCommutativeEncryptionRequest;
using ::wfanet::panelmatch::protocol::crypto::
    ReApplyCommutativeEncryptionResponse;

absl::StatusOr<std::string> ApplyCommutativeEncryptionWrapper(
    const std::string& serialized_request) {
  ApplyCommutativeEncryptionRequest request_proto;
  RETURN_IF_ERROR(ParseRequestFromString(request_proto, serialized_request));
  ASSIGN_OR_RETURN(ApplyCommutativeEncryptionResponse result,
                   ApplyCommutativeEncryption(request_proto));
  return result.SerializeAsString();
}

absl::StatusOr<std::string> ReApplyCommutativeEncryptionWrapper(
    const std::string& serialized_request) {
  ReApplyCommutativeEncryptionRequest request_proto;
  RETURN_IF_ERROR(ParseRequestFromString(request_proto, serialized_request));
  ASSIGN_OR_RETURN(ReApplyCommutativeEncryptionResponse result,
                   ReApplyCommutativeEncryption(request_proto));
  return result.SerializeAsString();
}

absl::StatusOr<std::string> ApplyCommutativeDecryptionWrapper(
    const std::string& serialized_request) {
  ApplyCommutativeDecryptionRequest request_proto;
  RETURN_IF_ERROR(ParseRequestFromString(request_proto, serialized_request));
  ASSIGN_OR_RETURN(ApplyCommutativeDecryptionResponse result,
                   ApplyCommutativeDecryption(request_proto));
  return result.SerializeAsString();
}

}  // namespace wfanet::panelmatch::protocol::crypto
