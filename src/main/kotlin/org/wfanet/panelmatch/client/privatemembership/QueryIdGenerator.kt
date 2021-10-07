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

import com.google.common.hash.BloomFilter
import com.google.common.hash.Funnels
import kotlin.random.Random

/** Generates [0, upperBound) in random order. */
@Suppress("UnstableApiUsage") // Guava "beta" is stable.
fun iterateUniqueQueryIds(upperBound: Int): Iterator<Int> = iterator {
  val seen = BloomFilter.create(Funnels.integerFunnel(), upperBound)

  repeat(upperBound) {
    val id = Random.nextInt(upperBound)
    if (!seen.mightContain(id)) {
      seen.put(id)
      yield(id)
    }
  }
}
