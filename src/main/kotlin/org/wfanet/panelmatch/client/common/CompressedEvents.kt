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

package org.wfanet.panelmatch.client.common

import com.google.protobuf.ByteString
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.apache.beam.sdk.values.PCollectionView
import org.wfanet.panelmatch.common.beam.map
import org.wfanet.panelmatch.common.beam.toSingletonView
import org.wfanet.panelmatch.common.compression.Compressor
import org.wfanet.panelmatch.common.compression.CompressorFactory

/**
 * The results of training a [Compressor][org.wfanet.panelmatch.common.compression.Compressor] and
 * then applying it to a [PCollection].
 */
data class CompressedEvents(
  val events: PCollection<KV<ByteString, ByteString>>,
  val dictionary: PCollection<ByteString>
) {
  fun makeCompressor(compressorFactory: CompressorFactory): PCollectionView<Compressor> {
    return dictionary
      .map("Make Compressor") { compressorFactory.build(it) }
      .toSingletonView("Make Compressor View")
  }
}
