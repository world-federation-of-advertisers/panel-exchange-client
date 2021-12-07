package org.wfanet.panelmatch.client.exchangetasks

import com.google.protobuf.ByteString
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.options.PipelineOptions
import org.apache.beam.sdk.options.PipelineOptionsFactory
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.panelmatch.client.common.ExchangeContext
import org.wfanet.panelmatch.client.privatemembership.CreateQueriesParameters
import org.wfanet.panelmatch.client.privatemembership.EvaluateQueriesParameters
import org.wfanet.panelmatch.client.privatemembership.PrivateMembershipCryptor
import org.wfanet.panelmatch.client.privatemembership.QueryEvaluator
import org.wfanet.panelmatch.client.privatemembership.QueryResultsDecryptor
import org.wfanet.panelmatch.client.storage.PrivateStorageSelector
import org.wfanet.panelmatch.common.ShardedFileName

class ApacheBeamTasks(
  private val getPrivateMembershipCryptor: (ByteString) -> PrivateMembershipCryptor,
  private val getQueryResultsEvaluator: (ByteString) -> QueryEvaluator,
  private val queryResultsDecryptor: QueryResultsDecryptor,
  private val privateStorageSelector: PrivateStorageSelector,
  private val pipelineOptions: PipelineOptions = PipelineOptionsFactory.create()
) : MapReduceTasks {

  override suspend fun ExchangeContext.getBuildPrivateMembershipQueriesTask(): ExchangeTask {
    check(step.stepCase == ExchangeWorkflow.Step.StepCase.BUILD_PRIVATE_MEMBERSHIP_QUERIES_STEP)
    val stepDetails = step.buildPrivateMembershipQueriesStep
    val privateMembershipCryptor = getPrivateMembershipCryptor(stepDetails.serializedParameters)

    val outputManifests =
      mapOf(
        "encrypted-queries" to stepDetails.encryptedQueryBundleFileCount,
        "query-to-join-keys-map" to stepDetails.queryIdAndPanelistKeyFileCount,
      )

    val parameters =
      CreateQueriesParameters(
        numShards = stepDetails.numShards,
        numBucketsPerShard = stepDetails.numBucketsPerShard,
        maxQueriesPerShard = stepDetails.numQueriesPerShard,
        padQueries = stepDetails.addPaddingQueries,
      )

    return apacheBeamTaskFor(outputManifests) {
      buildPrivateMembershipQueries(parameters, privateMembershipCryptor)
    }
  }

  override suspend fun ExchangeContext.getExecutePrivateMembershipQueriesTask(): ExchangeTask {
    check(step.stepCase == ExchangeWorkflow.Step.StepCase.EXECUTE_PRIVATE_MEMBERSHIP_QUERIES_STEP)
    val stepDetails = step.executePrivateMembershipQueriesStep

    val parameters =
      EvaluateQueriesParameters(
        numShards = stepDetails.numShards,
        numBucketsPerShard = stepDetails.numBucketsPerShard,
        maxQueriesPerShard = stepDetails.maxQueriesPerShard
      )

    val queryResultsEvaluator = getQueryResultsEvaluator(stepDetails.serializedParameters)

    val outputManifests = mapOf("encrypted-results" to stepDetails.encryptedQueryResultFileCount)

    return apacheBeamTaskFor(outputManifests) {
      executePrivateMembershipQueries(parameters, queryResultsEvaluator)
    }
  }

  override suspend fun ExchangeContext.getDecryptMembershipResultsTask(): ExchangeTask {
    check(
      step.stepCase == ExchangeWorkflow.Step.StepCase.DECRYPT_PRIVATE_MEMBERSHIP_QUERY_RESULTS_STEP
    )
    val stepDetails = step.decryptPrivateMembershipQueryResultsStep

    val outputManifests = mapOf("decrypted-event-data" to stepDetails.decryptEventDataSetFileCount)

    return apacheBeamTaskFor(outputManifests) {
      decryptPrivateMembershipResults(stepDetails.serializedParameters, queryResultsDecryptor)
    }
  }

  private suspend fun ExchangeContext.apacheBeamTaskFor(
    outputManifests: Map<String, Int>,
    execute: suspend ApacheBeamContext.() -> Unit
  ): ApacheBeamTask {
    return ApacheBeamTask(
      Pipeline.create(pipelineOptions),
      privateStorageSelector.getStorageFactory(exchangeDateKey),
      step.inputLabelsMap,
      outputManifests.mapValues { (k, v) -> ShardedFileName(step.outputLabelsMap.getValue(k), v) },
      execute
    )
  }
}
