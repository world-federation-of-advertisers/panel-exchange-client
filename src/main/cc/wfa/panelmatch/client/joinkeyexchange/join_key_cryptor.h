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

#ifndef SRC_MAIN_CC_WFA_PANELMATCH_CLIENT_JOINKEYEXCHANGE_JOIN_KEY_CRYPTOR_H_
#define SRC_MAIN_CC_WFA_PANELMATCH_CLIENT_JOINKEYEXCHANGE_JOIN_KEY_CRYPTOR_H_

#include "absl/status/statusor.h"
#include "wfa/panelmatch/client/joinkeyexchange/join_key_cryptor.pb.h"

namespace wfa::panelmatch::client::joinkeyexchange {

absl::StatusOr<JoinKeyCryptorGenerateCipherKeyResponse>
JoinKeyGenerateCipherKey(const JoinKeyCryptorGenerateCipherKeyRequest& request);

absl::StatusOr<JoinKeyCryptorEncryptResponse> JoinKeyEncrypt(
    const JoinKeyCryptorEncryptRequest& request);

absl::StatusOr<JoinKeyCryptorReEncryptResponse> JoinKeyReEncrypt(
    const JoinKeyCryptorReEncryptRequest& request);

absl::StatusOr<JoinKeyCryptorDecryptResponse> JoinKeyDecrypt(
    const JoinKeyCryptorDecryptRequest& request);

}  // namespace wfa::panelmatch::client::joinkeyexchange

#endif  // SRC_MAIN_CC_WFA_PANELMATCH_CLIENT_JOINKEYEXCHANGE_JOIN_KEY_CRYPTOR_H_
