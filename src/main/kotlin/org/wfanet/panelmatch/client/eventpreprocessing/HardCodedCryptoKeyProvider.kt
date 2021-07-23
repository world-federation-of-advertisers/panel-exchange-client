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
import org.apache.beam.sdk.transforms.SerializableFunction

/**
 * Takes in a cryptokey as ByteString and outputs the same ByteString
 *
 * Security concerns this introduces: 1) The crypto key could get logged. If logs are visible to
 * ```
 *      engineers, this could be vulnerable to insider risk.
 * ```
 * 2) The crypto key will reside in memory for longer. Other
 * ```
 *      processes on the machines executing the Apache Beam could
 *      potentially compromise it.
 * ```
 * 3) The crypto key will be serialized and sent between Apache
 * ```
 *      Beam workers. This means that vulnerable temporary files
 *      or network connections could leak key material.
 * ```
 */
class HardCodedCryptoKeyProvider(private val cryptoKey: ByteString) :
  SerializableFunction<Void?, ByteString> {
  override fun apply(void: Void?): ByteString {
    return cryptoKey
  }
}
