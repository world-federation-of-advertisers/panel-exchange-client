package org.wfanet.panelmatch.client.eventpreprocessing

import org.apache.beam.sdk.transforms.DoFn
import org.apache.beam.sdk.transforms.ParDo
import org.apache.beam.sdk.transforms.SerializableFunction
import org.apache.beam.sdk.values.PCollection
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.panelmatch.client.eventpreprocessing.BatchingDoFn as BatchingDoFn
import org.wfanet.panelmatch.common.beam.testing.BeamTestBase
import org.wfanet.panelmatch.common.beam.testing.assertThat

/** Unit tests for [BatchingDoFn]. */
@RunWith(JUnit4::class)
class BatchingDoFnTest : BeamTestBase() {

  private val collection: PCollection<String> by lazy {
    pcollectionOf("collection1", "1", "2", "3")
  }
  private val collection2: PCollection<String> by lazy {
    pcollectionOf("collection12", "1", "2", "345")
  }
  @Test
  fun testBatching() {
    val batchSize: Int = 1
    val doFn: DoFn<String, MutableList<String>> = BatchingDoFn<String>(batchSize, StringLengthSize)
    val result: PCollection<MutableList<String>> = collection.apply(ParDo.of(doFn))
    assertThat(result)
      .containsInAnyOrder(mutableListOf("1"), mutableListOf("2"), mutableListOf("3"))
  }
}

private object StringLengthSize : SerializableFunction<String, Int> {
  override fun apply(s: String): Int {
    return s.length
  }
}
