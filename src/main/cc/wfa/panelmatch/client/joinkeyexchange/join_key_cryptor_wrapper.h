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

#ifndef SRC_MAIN_CC_WFA_PANELMATCH_CLIENT_JOINKEYEXCHANGE_JOIN_KEY_CRYPTOR_WRAPPER_H_
#define SRC_MAIN_CC_WFA_PANELMATCH_CLIENT_JOINKEYEXCHANGE_JOIN_KEY_CRYPTOR_WRAPPER_H_

#include <string>

#include "absl/status/statusor.h"

// Wrapper methods used to generate the swig/JNI Java classes.
// The only functionality of these methods are converting between proto messages
// and their corresponding serialized strings, and then calling into the
// commutative_encryption methods.
namespace wfa::panelmatch::client::joinkeyexchange {

absl::StatusOr<std::string> JoinKeyCryptorGenerateCipherKeyWrapper(
    const std::string& serialized_request);

absl::StatusOr<std::string> JoinKeyCryptorEncryptWrapper(
    const std::string& serialized_request);

absl::StatusOr<std::string> JoinKeyCryptorReEncryptWrapper(
    const std::string& serialized_request);

absl::StatusOr<std::string> JoinKeyCryptorDecryptWrapper(
    const std::string& serialized_request);

}  // namespace wfa::panelmatch::client::joinkeyexchange

#endif  // SRC_MAIN_CC_WFA_PANELMATCH_CLIENT_JOINKEYEXCHANGE_JOIN_KEY_CRYPTOR_WRAPPER_H_
