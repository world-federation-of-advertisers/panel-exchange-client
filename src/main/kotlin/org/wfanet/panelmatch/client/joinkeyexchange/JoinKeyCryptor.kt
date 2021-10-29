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

package org.wfanet.panelmatch.client.joinkeyexchange

import com.google.protobuf.ByteString

/** Core deterministic, commutative cryptographic operations. */
interface JoinKeyCryptor {

  /** Generates privateKey. */
  fun generateKey(): ByteString

  /** Encrypts plaintexts. */
  fun encrypt(privateKey: ByteString, plaintexts: List<JoinKeyAndId>): List<JoinKeyAndId>

  /** Adds an additional layer of encryption to ciphertexts. */
  fun reEncrypt(privateKey: ByteString, ciphertexts: List<JoinKeyAndId>): List<JoinKeyAndId>

  /** Removes a layer of encryption from ciphertexts. */
  fun decrypt(privateKey: ByteString, ciphertexts: List<JoinKeyAndId>): List<JoinKeyAndId>
}
