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

// cue cmd dump src/main/k8s/edp_example_daemon_gke.cue >
// src/main/k8s/edp_example_daemon_gke.yaml

package k8s

_container_registry:  string @tag("container_registry")
_image_repo_prefix:   string @tag("image_repo_prefix")
_secret_name:         string @tag("secret_name")
_daemon_id:						string @tag("daemon_id")
_party_type:          string @tag("party_type")
_tink_key_uri:        string @tag("tink_key_uri")
_private_ca_name:     string @tag("private_ca_name")
_private_ca_pool_id:  string @tag("private_ca_pool_id")
_private_ca_location: string @tag("private_ca_location")


_party_type_flag:          "--party-type=\(_party_type)"
_private_ca_name_flag:     "--privateca-ca-name=\(_private_ca_name)"
_private_ca_pool_flag:     "--privateca-pool-id=\(_private_ca_pool_id)"
_private_ca_location_flag: "--privateca-ca-location=\(_private_ca_location)"

#GloudProject:            "ads-open-measurement"
#SpannerInstance:         "halo-panelmatch-demo-instance"
#CloudStorageBucket:      "halo-panel-dev-bucket"
#KingdomPublicApiTarget:  "public.kingdom.dev.halo-cmm.org:8443"
#ContainerRegistryPrefix: _container_registry + "/" + _image_repo_prefix
#DefaultResourceConfig: {
	replicas:              1
	resourceRequestCpu:    "100m"
	resourceLimitCpu:      "400m"
	resourceRequestMemory: "256Mi"
	resourceLimitMemory:   "512Mi"
}

_tink_key_uri_flags: [
	"--tink-key-uri=\(_tink_key_uri)",
	"--tink-credential-path=\(_tink_key_credential_path)"
]

_exchange_api_flags: [
		"--exchange-api-target=" + (#Target & {name: "v2alpha-public-api-server"}).target,
		"--exchange-api-cert-host=localhost",
	]

example_daemon_deployment: "example_daemon_deployment": #Deployment & {
	_name:            "example-panel-exchange-daemon"
	_image:           #ContainerRegistryPrefix + "/example-panel-exchange-daemon"
	_jvmFlags:        "-Xmx12g -Xms2g"
	_imagePullPolicy: "Always"
	_resourceConfig:  #DefaultResourceConfig
	_secretName:      _secret_name // "certs-and-configs-cct246f859"

	_args:
	_exchange_api_flags +
	_tink_key_uri_flags +
	[
		_party_type_flag,
		_private_ca_name_flag,
		_private_ca_pool_flag,
		_private_ca_location_flag,
		"--id=\(_daemon_id)",
		"--tls-cert-file=/var/run/secrets/files/mc_tls.pem",
		"--tls-key-file=/var/run/secrets/files/mc_tls.key",
		"--cert-collection-file=/var/run/secrets/files/all_root_certs.pem",
		"--blob-size-limit-bytes=1000000000",
		"--storage-signing-algorithm=EC",
		"--task-timeout=24h",
		"--google-cloud-storage-bucket=" + #CloudStorageBucket,
		"--google-cloud-storage-project=" + #GloudProject,
		"--channel-shutdown-timeout=3s",
		"--polling-interval=1m",
		"--preprocessing-max-byte-size=1000000",
		"--preprocessing-file-count=1000",
		"--x509-common-name=SomeCommonName",
		"--x509-organization=SomeOrganization",
		"--x509-dns-name=example.com",
		"--x509-valid-days=365",
		"--privateca-project-id=" + #GloudProject,
	]
}
