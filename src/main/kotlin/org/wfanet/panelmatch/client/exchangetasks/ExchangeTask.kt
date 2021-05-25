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

/** Interface for ExchangeTask. */
interface ExchangeTask {
  /**
   * Executes subclass on input map and returns the output.
   *
   * @param input inputs specified by [task].
   * @param sendDebugLog function which writes logs happened during execution.
   * @return Executed output. It is a map from the labels to the payload associated with the label.
   */
  suspend fun execute(
    input: Map<String, ByteString>
  ): Map<String, ByteString>
}
