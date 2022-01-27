// Copyright 2022 The Cross-Media Measurement Authors
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

package org.wfanet.panelmatch.client.deploy.example.gcloud

import com.google.crypto.tink.integration.gcpkms.GcpKmsClient
import java.util.Optional
import org.wfanet.measurement.storage.StorageClient
import org.wfanet.panelmatch.client.deploy.DaemonStorageClientDefaults

class GcpDaemonStorageClientDefaults(rootStorageClient: StorageClient, tinkKeyUri: String) :
  DaemonStorageClientDefaults(rootStorageClient, tinkKeyUri) {

  companion object {
    init {
      GcpKmsClient.register(Optional.empty(), Optional.empty())
    }
  }
}
