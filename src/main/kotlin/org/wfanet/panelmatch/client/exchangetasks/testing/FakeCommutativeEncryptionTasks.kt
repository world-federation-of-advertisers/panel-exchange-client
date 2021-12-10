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
import org.wfanet.panelmatch.client.exchangetasks.CommutativeEncryptionTasks

class FakeCommutativeEncryptionTasks : CommutativeEncryptionTasks {
  override fun encrypt(context: ExchangeContext) = FakeExchangeTask("encrypt")
  override fun reEncrypt(context: ExchangeContext) = FakeExchangeTask("re-encrypt")
  override fun decrypt(context: ExchangeContext) = FakeExchangeTask("decrypt")
  override fun generateEncryptionKey(context: ExchangeContext) =
    FakeExchangeTask("generate-encryption-key")
}
