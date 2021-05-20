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
#include "wfa/panelmatch/crypto/cryptor.pb.h"
#include "wfanet/panelmatch/common/crypto/encryption_utility_helper.h"
#include "wfanet/panelmatch/protocol/crypto/commutative_encryption_utility.h"

namespace wfanet::panelmatch::protocol::crypto {

using ::wfa::panelmatch::crypto::ApplyCommutativeDecryptionRequest;
using ::wfa::panelmatch::crypto::ApplyCommutativeDecryptionResponse;
using ::wfa::panelmatch::crypto::ApplyCommutativeEncryptionRequest;
using ::wfa::panelmatch::crypto::ApplyCommutativeEncryptionResponse;
using ::wfa::panelmatch::crypto::ReApplyCommutativeEncryptionRequest;
using ::wfa::panelmatch::crypto::ReApplyCommutativeEncryptionResponse;
using ::wfanet::panelmatch::common::crypto::ParseRequestFromString;

absl::StatusOr<std::string> ApplyCommutativeEncryptionWrapper(
    const std::string& serialized_request) {
  ApplyCommutativeEncryptionRequest request_proto;
  RETURN_IF_ERROR(ParseRequestFromString(serialized_request, request_proto));
  ASSIGN_OR_RETURN(ApplyCommutativeEncryptionResponse result,
                   ApplyCommutativeEncryption(request_proto));
  return result.SerializeAsString();
}

absl::StatusOr<std::string> ReApplyCommutativeEncryptionWrapper(
    const std::string& serialized_request) {
  ReApplyCommutativeEncryptionRequest request_proto;
  RETURN_IF_ERROR(ParseRequestFromString(serialized_request, request_proto));
  ASSIGN_OR_RETURN(ReApplyCommutativeEncryptionResponse result,
                   ReApplyCommutativeEncryption(request_proto));
  return result.SerializeAsString();
}

absl::StatusOr<std::string> ApplyCommutativeDecryptionWrapper(
    const std::string& serialized_request) {
  ApplyCommutativeDecryptionRequest request_proto;
  RETURN_IF_ERROR(ParseRequestFromString(serialized_request, request_proto));
  ASSIGN_OR_RETURN(ApplyCommutativeDecryptionResponse result,
                   ApplyCommutativeDecryption(request_proto));
  return result.SerializeAsString();
}

}  // namespace wfanet::panelmatch::protocol::crypto
