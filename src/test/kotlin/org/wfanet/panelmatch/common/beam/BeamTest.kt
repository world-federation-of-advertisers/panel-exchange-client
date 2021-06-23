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

package org.wfanet.panelmatch.common.beam

import com.google.common.truth.Truth.assertThat
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.apache.beam.sdk.values.PCollectionList
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.panelmatch.common.beam.testing.BeamTestBase
import org.wfanet.panelmatch.common.beam.testing.assertThat

@RunWith(JUnit4::class)
class BeamTest : BeamTestBase() {
  private val collection: PCollection<KV<Int, String>> by lazy {
    pcollectionOf("collection", 1 toKv "A", 2 toKv "B", 3 toKv "C")
  }

  @Test
  fun keys() {
    assertThat(collection.keys()).containsInAnyOrder(1, 2, 3)
  }

  @Test
  fun values() {
    assertThat(collection.values()).containsInAnyOrder("A", "B", "C")
  }

  @Test
  fun parDo() {
    val result: PCollection<Int> =
      collection.parDo {
        yield(it.key + 10)
        yield(it.key + 100)
      }

    assertThat(result).containsInAnyOrder(11, 101, 12, 102, 13, 103)
  }

  @Test
  fun map() {
    assertThat(collection.map { it.key + 10 }).containsInAnyOrder(11, 12, 13)
  }

  @Test
  fun flatMap() {
    assertThat(collection.flatMap { listOf(it.key + 10, it.key + 100) })
      .containsInAnyOrder(11, 12, 13, 101, 102, 103)
  }

  @Test
  fun keyBy() {
    assertThat(pcollectionOf("unkeyed-items", 1, 2, 3).keyBy { it + 10 })
      .containsInAnyOrder(11 toKv 1, 12 toKv 2, 13 toKv 3)
  }

  @Test
  fun mapKeys() {
    assertThat(collection.mapKeys { -it }).containsInAnyOrder(-1 toKv "A", -2 toKv "B", -3 toKv "C")
  }

  @Test
  fun mapValues() {
    assertThat(collection.mapValues { it.toLowerCase() })
      .containsInAnyOrder(1 toKv "a", 2 toKv "b", 3 toKv "c")
  }

  @Test
  fun partition() {
    val parts: PCollectionList<KV<Int, String>> = collection.partition(2) { it.key % 2 }
    assertThat(parts.size()).isEqualTo(2)
    assertThat(parts[0]).containsInAnyOrder(2 toKv "B")
    assertThat(parts[1]).containsInAnyOrder(1 toKv "A", 3 toKv "C")
  }

  @Test
  fun join() {
    val rightHandSide = pcollectionOf("right-hand side", 1 toKv 'a', 1 toKv 'b', 4 toKv 'c')
    val result: PCollection<KV<Int, String>> =
      collection.join(rightHandSide) { key, lefts, rights ->
        val leftString = lefts.sorted().joinToString(", ")
        val rightString = rights.sorted().joinToString(", ")
        yield(key toKv "[$leftString] and [$rightString]")
      }
    assertThat(result)
      .containsInAnyOrder(
        1 toKv "[A] and [a, b]",
        2 toKv "[B] and []",
        3 toKv "[C] and []",
        4 toKv "[] and [c]"
      )
  }

  @Test
  fun joinIgnoringArguments() {
    val rightHandSide = pcollectionOf("right-hand side", 1 toKv 'a', 1 toKv 'b', 4 toKv 'c')
    val result: PCollection<Int> = collection.join(rightHandSide) { _, _, _ -> yield(1) }
    assertThat(result).containsInAnyOrder(1, 1, 1, 1)
  }

  @Test
  fun count() {
    assertThat(collection.count()).containsInAnyOrder(3L)
  }

  @Test
  fun toPCollectionList() {
    val list =
      listOf(pcollectionOf("first", 1, 2, 3), pcollectionOf("second", 4, 5, 6)).toPCollectionList()
    assertThat(list.size()).isEqualTo(2)
    assertThat(list[0]).containsInAnyOrder(1, 2, 3)
    assertThat(list[1]).containsInAnyOrder(4, 5, 6)
  }

  @Test
  fun flatten() {
    val list =
      listOf(pcollectionOf("first", 1, 2, 3), pcollectionOf("second", 4, 5, 6)).toPCollectionList()
    assertThat(list.flatten()).containsInAnyOrder(1, 2, 3, 4, 5, 6)
  }

  @Test
  fun parDoWithSideInput() {
    val sideInput = collection.count()
    val result: PCollection<KV<Int, Long>> =
      collection.parDoWithSideInput(sideInput) { element, count -> yield(element.key toKv count) }
    assertThat(result).containsInAnyOrder(1 toKv 3L, 2 toKv 3L, 3 toKv 3L)
  }
}
