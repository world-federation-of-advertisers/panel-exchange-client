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

package org.wfanet.panelmatch.client.exchangetasks

import com.google.protobuf.ByteString
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.wfanet.panelmatch.client.privatemembership.ReadAsSingletonPCollection
import org.wfanet.panelmatch.client.privatemembership.ReadShardedData
import org.wfanet.panelmatch.client.privatemembership.WriteShardedData
import org.wfanet.panelmatch.client.storage.StorageFactory
import org.wfanet.panelmatch.client.storage.VerifiedStorageClient.VerifiedBlob
import org.wfanet.panelmatch.common.ShardedFileName

/** Base class for Apache Beam-running ExchangeTasks. */
abstract class ApacheBeamTask : ExchangeTask {
  protected abstract val storageFactory: StorageFactory

  protected val pipeline: Pipeline = Pipeline.create()

  protected suspend fun readFromManifest(manifest: VerifiedBlob): PCollection<ByteString> {
    val shardedFileName = manifest.toStringUtf8()
    return pipeline.apply("Read $shardedFileName", ReadShardedData(shardedFileName, storageFactory))
  }

  protected fun readSingleBlobAsPCollection(blobKey: String): PCollection<ByteString> {
    return pipeline.apply("Read $blobKey", ReadAsSingletonPCollection(blobKey, storageFactory))
  }

  // TODO: consider also adding a helper to write non-sharded files.
  protected fun PCollection<KV<Int, ByteString>>.write(shardedFileName: ShardedFileName) {
    apply("Write ${shardedFileName.spec}", WriteShardedData(shardedFileName.spec, storageFactory))
  }
}
