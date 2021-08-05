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

package org.wfanet.panelmatch.common.crypto

import com.google.protobuf.ByteString
import java.security.MessageDigest
import java.util.Base64

const val HASH_ALGORITHM = "SHA-256"

/** Generates a SHA-256 of [data] using [salt] */
fun hashSha256(data: ByteString, salt: ByteString): ByteString {
  val sha256MessageDigest = MessageDigest.getInstance(HASH_ALGORITHM)
  sha256MessageDigest.update(salt.toByteArray())
  return ByteString.copyFrom(sha256MessageDigest.digest(data.toByteArray()))
}

/** Generates a SHA-256 of [data] without a salt */
fun hashSha256(data: ByteString): ByteString {
  val sha256MessageDigest = MessageDigest.getInstance(HASH_ALGORITHM)
  return ByteString.copyFrom(sha256MessageDigest.digest(data.toByteArray()))
}

/** Generates a SHA-256 of [data] without a salt */
fun hashSha256ToLong(data: ByteString): Long {
  val sha256MessageDigest = MessageDigest.getInstance(HASH_ALGORITHM)
  return Base64.getEncoder().encodeToString(requireNotNull(sha256MessageDigest.digest(data.toByteArray()))).toLong()
}
