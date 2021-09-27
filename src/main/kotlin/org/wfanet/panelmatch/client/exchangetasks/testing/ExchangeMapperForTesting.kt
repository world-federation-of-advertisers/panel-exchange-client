package org.wfanet.panelmatch.client.exchangetasks.testing

import com.google.protobuf.ByteString
import kotlinx.coroutines.flow.Flow
import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow
import org.wfanet.measurement.common.asBufferedFlow
import org.wfanet.measurement.common.flatten
import org.wfanet.panelmatch.client.exchangetasks.ExchangeTask
import org.wfanet.panelmatch.client.exchangetasks.ExchangeTaskDetails
import org.wfanet.panelmatch.client.exchangetasks.ExchangeTaskMapper
import org.wfanet.panelmatch.client.storage.VerifiedStorageClient
import org.wfanet.panelmatch.common.toByteString

class ExchangeMapperForTesting() : ExchangeTaskMapper {

  override suspend fun getExchangeTaskForStep(step: ExchangeWorkflow.Step): ExchangeTaskDetails {
    return ExchangeTaskDetails(
      exchangeTask =
        object : ExchangeTask {
          override suspend fun execute(
            input: Map<String, VerifiedStorageClient.VerifiedBlob>
          ): Map<String, Flow<ByteString>> {
            return input.mapKeys { "Out:${it.key}" }.mapValues {
              val valString: String = it.value.read(1024).flatten().toStringUtf8()
              "Out:$valString".toByteString().asBufferedFlow(1024)
            }
          }
        }
    )
  }
}
