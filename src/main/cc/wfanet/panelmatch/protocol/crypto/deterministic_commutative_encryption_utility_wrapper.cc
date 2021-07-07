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

#include "wfanet/panelmatch/protocol/crypto/deterministic_commutative_encryption_utility_wrapper.h"

#include "absl/status/statusor.h"
#include "wfanet/panelmatch/common/jni_wrap.h"
#include "wfanet/panelmatch/protocol/crypto/cryptor.pb.h"
#include "wfanet/panelmatch/protocol/crypto/deterministic_commutative_encryption_utility.h"

namespace wfanet::panelmatch::protocol::crypto {
using ::wfanet::panelmatch::common::JniWrap;
using ::wfanet::panelmatch::protocol::protobuf::CryptorDecryptRequest;
using ::wfanet::panelmatch::protocol::protobuf::CryptorDecryptResponse;
using ::wfanet::panelmatch::protocol::protobuf::CryptorEncryptRequest;
using ::wfanet::panelmatch::protocol::protobuf::CryptorEncryptResponse;
using ::wfanet::panelmatch::protocol::protobuf::CryptorReEncryptRequest;
using ::wfanet::panelmatch::protocol::protobuf::CryptorReEncryptResponse;

absl::StatusOr<std::string> DeterministicCommutativeEncryptWrapper(
    const std::string& serialized_request) {
  return JniWrap(serialized_request, DeterministicCommutativeEncrypt);
}

absl::StatusOr<std::string> DeterministicCommutativeReEncryptWrapper(
    const std::string& serialized_request) {
  return JniWrap(serialized_request, DeterministicCommutativeReEncrypt);
}

absl::StatusOr<std::string> DeterministicCommutativeDecryptWrapper(
    const std::string& serialized_request) {
  return JniWrap(serialized_request, DeterministicCommutativeDecrypt);
}

}  // namespace wfanet::panelmatch::protocol::crypto
