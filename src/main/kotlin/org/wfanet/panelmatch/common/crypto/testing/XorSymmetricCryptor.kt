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

package org.wfanet.panelmatch.common.crypto.testing

import com.google.protobuf.ByteString
import kotlin.experimental.xor
import org.wfanet.panelmatch.common.crypto.SymmetricCryptor

/** Does no real crypto. It only xors [data] with [privateKey]. */
class XorSymmetricCryptor : SymmetricCryptor {

  /** Assuming the receiver is UTF8, converts it into a [ByteString]. */
  fun ByteString.xor(privateKey: ByteString): ByteString {
    require(privateKey.size() > 0) { "Length of private key must be greater than zero" }
    return ByteString.copyFrom(
      this.toByteArray().zip(privateKey.toByteArray()).map { (a, b) -> a.xor(b) }.toByteArray() +
        this.toByteArray().copyOfRange(privateKey.size(), this.size())
    )
  }

  override fun encrypt(privateKey: ByteString, data: ByteString): ByteString {
    return data.xor(privateKey)
  }

  override fun decrypt(privateKey: ByteString, data: ByteString): ByteString {
    return data.xor(privateKey)
  }
}
