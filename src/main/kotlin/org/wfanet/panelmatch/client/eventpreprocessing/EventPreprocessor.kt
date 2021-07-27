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

package org.wfanet.panelmatch.client.eventpreprocessing

import com.google.protobuf.ByteString
import org.apache.beam.sdk.transforms.ParDo
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection

/** Runs preprocessing DoFns on input [PCollection] and outputs encrypted [PCollection] */
fun eventPreprocessor(
  collection: PCollection<KV<ByteString, ByteString>>,
  maxByteSize: Int,
  pepper: ByteString,
  cryptokey: ByteString
): PCollection<KV<Long, ByteString>> {
  return collection
    .apply(ParDo.of(BatchingDoFn(maxByteSize, EventSize)))
    .apply(
      ParDo.of(
        EncryptionEventsDoFn(
          EncryptEvents(),
          HardCodedPepperProvider(pepper),
          HardCodedCryptoKeyProvider(cryptokey)
        )
      )
    )
}