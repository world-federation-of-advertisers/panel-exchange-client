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

import java.security.cert.X509Certificate
import org.apache.beam.sdk.Pipeline
import org.wfanet.panelmatch.client.exchangetasks.WorkflowExchangeTask.Workflow.WorkflowOutputs
import org.wfanet.panelmatch.common.beam.ReadSignedFiles

class ExecutePrivateMembershipQueriesWorkflow(private val x509Certificate: X509Certificate) :
  WorkflowExchangeTask.Workflow {
  override suspend fun execute(
    outputUriPrefix: String,
    inputUris: Map<String, String>
  ): WorkflowOutputs {
    val pipeline = Pipeline.create()
    pipeline.apply(ReadSignedFiles(inputUris["something"]!!, x509Certificate))
    pipeline.run()
    return WorkflowOutputs(emptyMap(), emptyMap())
  }
}
