package org.wfanet.panelmatch.client.exchangetasks.testing

import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.toByteStringUtf8
import kotlinx.coroutines.flow.Flow
import org.wfanet.measurement.common.asBufferedFlow
import org.wfanet.measurement.storage.StorageClient
import org.wfanet.panelmatch.client.exchangetasks.ExchangeTask
import org.wfanet.panelmatch.common.storage.toStringUtf8

class FakeCryptorExchangeTask(val task: String) : ExchangeTask {
  override suspend fun execute(
    input: Map<String, StorageClient.Blob>
  ): Map<String, Flow<ByteString>> {
    return input.mapKeys { "Out:${it.key}" }.mapValues {
      val valString: String = it.value.toStringUtf8()
      "Out:$task-$valString".toByteStringUtf8().asBufferedFlow(1024)
    }
  }
}
