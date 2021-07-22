package org.wfanet.panelmatch.client.batchlookup.testing

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import kotlin.random.Random
import org.apache.beam.sdk.transforms.Create
import org.junit.Before
import org.junit.Test
import org.wfanet.panelmatch.client.batchlookup.BatchLookupWorkflow
import org.wfanet.panelmatch.client.batchlookup.BatchLookupWorkflow.Parameters
import org.wfanet.panelmatch.client.batchlookup.Bucketing
import org.wfanet.panelmatch.client.batchlookup.DatabaseKey
import org.wfanet.panelmatch.client.batchlookup.Plaintext
import org.wfanet.panelmatch.client.batchlookup.QueryBundle
import org.wfanet.panelmatch.client.batchlookup.QueryEvaluator
import org.wfanet.panelmatch.client.batchlookup.QueryId
import org.wfanet.panelmatch.client.batchlookup.registerPirCoders
import org.wfanet.panelmatch.common.beam.testing.BeamTestBase
import org.wfanet.panelmatch.common.beam.testing.assertThat

private val random = Random(seed = 12345L)

/** Base test class for testing the full pipeline, including a specific [QueryEvaluator]. */
abstract class AbstractPipelineEndToEndTest : BeamTestBase() {
  abstract val queryEvaluator: QueryEvaluator
  abstract val helper: QueryEvaluatorTestHelper

  @Before
  fun registerCoders() {
    pipeline.registerPirCoders()
  }

  @Test
  fun endToEnd() {
    for (numShards in listOf(1, 10, 100)) {
      for (numBucketsPerShard in listOf(1, 10, 100, 1000)) {
        for (subshardSizeBytes in listOf(1, 10, 100)) {
          val parameters =
            Parameters(
              numShards = numShards,
              numBucketsPerShard = numBucketsPerShard,
              subshardSizeBytes = subshardSizeBytes
            )
          runEndToEndTest(parameters)
        }
      }
    }
  }

  private fun runEndToEndTest(parameters: Parameters) {
    val keys: List<Long> = (0 until 10).map { random.nextLong() }
    val rawDatabase: Map<Long, ByteString> =
      keys.associateWith { ByteString.copyFromUtf8("<this is the payload for $it>") }

    val database: Map<DatabaseKey, Plaintext> =
      rawDatabase.mapKeys { DatabaseKey(it.key) }.mapValues { Plaintext(it.value) }
    val databasePCollection = pipeline.apply("Create Database", Create.of(database))

    val rawQueries = keys.take(3).mapIndexed { i, key -> key to QueryId(i) }
    val expectedResults: List<String> = rawQueries.map { rawDatabase[it.first]!!.toStringUtf8() }

    val bucketing =
      Bucketing(
        numShards = parameters.numShards,
        numBucketsPerShard = parameters.numBucketsPerShard
      )
    val bucketsAndShardsToQuery = rawQueries.map { bucketing.apply(it.first) to it.second }
    val queryBundles: List<QueryBundle> =
      bucketsAndShardsToQuery.groupBy { it.first.first }.map { (shard, entries) ->
        helper.makeQueryBundle(shard, entries.map { it.second to it.first.second })
      }
    val queryBundlesPCollection = pipeline.apply("Create QueryBundles", Create.of(queryBundles))

    val workflow = BatchLookupWorkflow(parameters, queryEvaluator)
    val results = workflow.batchLookup(databasePCollection, queryBundlesPCollection)
    val localHelper = helper // For Beam's serialization
    assertThat(results).satisfies {
      val uniqueResults =
        it
          .flatMap { result ->
            val data = localHelper.decodeResultData(result).toStringUtf8()
            Regex("(<[^>]+>)").findAll(data).map { match -> match.groupValues[1] }.toList()
          }
          .toSet()
      assertThat(uniqueResults).containsAtLeastElementsIn(expectedResults)
      null
    }
  }
}
