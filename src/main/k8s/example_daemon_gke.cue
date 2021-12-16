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

// cue cmd dump src/main/k8s/example_daemon_from_cue.cue >
// src/main/k8s/example_daemon_from_cue.yaml

package k8s

example_daemon_deployment: "example_daemon_deployment": #Deployment & {
  _name:       "example-panel-exchange-daemon"
	_replicas:   1
	_image:      "gcr.io/cloud-example-panel-exchange-daemon"
	_jvm_flags: "-Xmx12g -Xms2g"
	_resourceRequestCpu:    "200m"
  _resourceLimitCpu:      "800m"
	_resourceRequestMemory: "4g"
	_resourceLimitMemory:   "16g"
  _args: [
    "--id=\(_daemon_id)",
    "--party-type=\(_daemon_party_type)",
    "--tink-key-uri=localhost",
    "--blob_size_limit_bytes=1GiB",
    "--storage-signing-algorithm=EC",
    "--task-timeout=24h",
    "--exchange-api-target=localhost",
    "--exchange-api-cert-host=localhost",
    "--google-cloud-storage-bucket=\(_cloud_storage_bucket)",
    "--google-cloud-storage-project=\(_cloud_storage_project)",
    "--channel-shutdown-timeout=3s",
    "--polling-interval=1m",
  ]
}