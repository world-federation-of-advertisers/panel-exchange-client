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

package org.wfanet.panelmatch.client.privatemembership

import java.io.Serializable
import org.wfanet.panelmatch.common.crypto.hashSha256ToSpace

/** Computes the appropriate bucket and shard for keys. */
class Bucketing(private val numShards: Int, private val numBucketsPerShard: Int) : Serializable {
  init {
    require(numShards > 0)
    require(numBucketsPerShard > 0)
  }

  /** Returns the [ShardId] and [BucketId] for [value]. */
  fun apply(value: Long): Pair<ShardId, BucketId> {
    return shard(value) to bucket(value)
  }

  /** Returns the hashed [ShardId] and [BucketId] for a [JoinKey]. */
  fun hashAndApply(joinKey: JoinKey): Pair<ShardId, BucketId> {
    val hashedValue: Long =
      hashSha256ToSpace(joinKey.key, numShards.toLong() * numBucketsPerShard.toLong())
    return apply(hashedValue)
  }

  private fun shard(value: Long): ShardId {
    val remainder = java.lang.Long.remainderUnsigned(value, numShards.toLong())
    // The conversion here is safe because 0 <= remainder < numShards and numShards is an Int.
    return shardIdOf(remainder.toInt())
  }

  private fun bucket(value: Long): BucketId {
    val quotient = java.lang.Long.divideUnsigned(value, numShards.toLong())
    val remainder = java.lang.Long.remainderUnsigned(quotient, numBucketsPerShard.toLong())

    check(remainder >= 0) {
      """
      Bucketing(numShards = $numShards, numBucketsPerShard = $numBucketsPerShard):
        value = $value (unsigned: ${java.lang.Long.toUnsignedString(quotient)})
        quotient = $quotient (unsigned: ${java.lang.Long.toUnsignedString(quotient)})
        remainder = $remainder (unsigned: ${java.lang.Long.toUnsignedString(remainder)})
      """.trimIndent()
    }

    // The conversion here is safe because 0 <= remainder < numBucketsPerShard and
    // numBucketsPerShard is an Int.
    return bucketIdOf(remainder.toInt())
  }
}
