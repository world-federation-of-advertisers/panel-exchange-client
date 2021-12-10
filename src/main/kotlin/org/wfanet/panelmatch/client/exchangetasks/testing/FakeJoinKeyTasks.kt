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

package org.wfanet.panelmatch.client.exchangetasks.testing

import org.wfanet.panelmatch.client.common.ExchangeContext
import org.wfanet.panelmatch.client.exchangetasks.JoinKeyTasks

class FakeJoinKeyTasks : JoinKeyTasks {
  override fun generateLookupKeys(context: ExchangeContext) = FakeExchangeTask("lookup-key")
  override fun generateSymmetricKey(context: ExchangeContext) = FakeExchangeTask("symmetric-key")
  override fun generateSerializedRlweKeys(context: ExchangeContext) =
    FakeExchangeTask("serialized-rlwe-keys")
  override fun generateExchangeCertificate(context: ExchangeContext) =
    FakeExchangeTask("exchange-certificate")
  override fun intersectAndValidate(context: ExchangeContext) =
    FakeExchangeTask("intersect-and-validate")
}
