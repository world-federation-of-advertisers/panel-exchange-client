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
