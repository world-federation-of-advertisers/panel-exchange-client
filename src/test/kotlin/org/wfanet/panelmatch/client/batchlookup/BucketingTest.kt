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

package org.wfanet.panelmatch.client.batchlookup

import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFails
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BucketingTest {
  @Test
  fun rejectsInvalidParameters() {
    assertFails { Bucketing(numShards = 0, numBucketsPerShard = 5) }
    assertFails { Bucketing(numShards = 0, numBucketsPerShard = 5) }
    assertFails { Bucketing(numShards = 5, numBucketsPerShard = 0) }
    assertFails { Bucketing(numShards = -5, numBucketsPerShard = 10) }
    assertFails { Bucketing(numShards = 5, numBucketsPerShard = -10) }
  }

  @Test
  fun `normal usage`() {
    val bucketing = Bucketing(numShards = 7, numBucketsPerShard = 3)
    assertThat(bucketing.apply(0)).isEqualTo(ShardId(0) to BucketId(0))
    assertThat(bucketing.apply(1)).isEqualTo(ShardId(1) to BucketId(0))

    // 9 % 7 == 2, (9 / 7) % 3 == 1
    assertThat(bucketing.apply(9)).isEqualTo(ShardId(2) to BucketId(1))

    // 1000 % 7 == 6, (1000 / 7) % 3 == 1
    assertThat(bucketing.apply(1000)).isEqualTo(ShardId(6) to BucketId(1))
  }

  @Test
  fun `math is unsigned`() {
    val bucketing = Bucketing(numShards = 7, numBucketsPerShard = 3)

    // -2^63 is 2^63 when interpreted as unsigned.
    // 2^63 % 7 == 1, (2^63 / 7) % 3 == 1
    assertThat(bucketing.apply(Long.MIN_VALUE)).isEqualTo(ShardId(1) to BucketId(1))

    // -1L is 2^64-1 when interpreted as unsigned.
    // (2^64-1) % 7 == 1, ((2^64-1) / 7) % 3 == 2
    assertThat(bucketing.apply(-1L)).isEqualTo(ShardId(1) to BucketId(2))
  }

  @Test
  fun `single shard`() {
    val bucketing = Bucketing(numShards = 1, numBucketsPerShard = 3)
    assertThat(bucketing.apply(0)).isEqualTo(ShardId(0) to BucketId(0))
    assertThat(bucketing.apply(1)).isEqualTo(ShardId(0) to BucketId(1))
    assertThat(bucketing.apply(2)).isEqualTo(ShardId(0) to BucketId(2))
    assertThat(bucketing.apply(3)).isEqualTo(ShardId(0) to BucketId(0))
    assertThat(bucketing.apply(10)).isEqualTo(ShardId(0) to BucketId(1))
  }

  @Test
  fun `single bucket`() {
    val bucketing = Bucketing(numShards = 50, numBucketsPerShard = 1)
    assertThat(bucketing.apply(0)).isEqualTo(ShardId(0) to BucketId(0))
    assertThat(bucketing.apply(1)).isEqualTo(ShardId(1) to BucketId(0))
    assertThat(bucketing.apply(53)).isEqualTo(ShardId(3) to BucketId(0))
  }
}
