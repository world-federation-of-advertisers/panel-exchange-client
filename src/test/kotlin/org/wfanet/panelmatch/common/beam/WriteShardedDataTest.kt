package org.wfanet.panelmatch.common.beam

import com.google.common.truth.Truth.assertWithMessage
import com.google.protobuf.stringValue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.panelmatch.client.storage.testing.InMemoryStorageFactory
import org.wfanet.panelmatch.common.ShardedFileName
import org.wfanet.panelmatch.common.beam.testing.BeamTestBase
import org.wfanet.panelmatch.common.testing.runBlockingTest

@RunWith(JUnit4::class)
class WriteShardedDataTest : BeamTestBase() {

  @Test
  fun addsAllFilesInFileSpec() = runBlockingTest {
    val shardedFileName = ShardedFileName("foo-*-of-10")
    val storageFactory = InMemoryStorageFactory()
    val input = pcollectionOf("Input", stringValue { value = "some-input" })
    input.apply(WriteShardedData(shardedFileName.spec, storageFactory))
    pipeline.run()

    val storageClient = storageFactory.build()
    for (filename in shardedFileName.fileNames) {
      assertWithMessage("Blob key $filename").that(storageClient.getBlob(filename)).isNotNull()
    }
  }
}
