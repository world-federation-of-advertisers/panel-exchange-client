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

import org.wfanet.panelmatch.client.common.ExchangeContext

/** JoinKey Tasks */
interface JoinKeyTasks {
  /** Returns the task that generates a symmetric key. */
  fun generateSymmetricKey(context: ExchangeContext): ExchangeTask

  /** Returns the task that generates lookup keys. */
  fun generateLookupKeys(context: ExchangeContext): ExchangeTask

  /** Returns the task that generates serialized rlwe keys. */
  fun generateSerializedRlweKeys(context: ExchangeContext): ExchangeTask

  /** Returns the task that generates a certificate. */
  fun generateExchangeCertificate(context: ExchangeContext): ExchangeTask

  /** Returns the task that validates the step. */
  fun intersectAndValidate(context: ExchangeContext): ExchangeTask
}
