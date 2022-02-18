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

_container_registry:     string @tag("container_registry")
_image_repo_prefix:      string @tag("image_repo_prefix")
_cloud_storage_bucket:   string @tag("cloud_storage_bucket")
_tink_key_uri:           string @tag("tink_key_uri")
_cloud_credentials_path: string @tag("cloud_credentials_path")
_secret_name:            string @tag("secret_name")
_daemon_id:              string @tag("daemon_id")
_recurring_exchange_id:  string @tag("recurring_exchange_id")
_private_ca_name:        string @tag("private_ca_name")
_private_ca_pool_id:     string @tag("private_ca_pool_id")
_private_ca_location:    string @tag("private_ca_location")

#GloudProject:            "halo-cmm-dev"
#SpannerInstance:         "panelmatch-dev-instance"
#KingdomPublicApiTarget:  "public.kingdom.dev.halo-cmm.org:8443"
#ContainerRegistryPrefix: _container_registry + "/" + _image_repo_prefix
#DefaultResourceConfig: {
	replicas:              1
	resourceRequestCpu:    "100m"
	resourceLimitCpu:      "400m"
	resourceRequestMemory: "256Mi"
	resourceLimitMemory:   "512Mi"
}

_private_ca_flags: [
	"--privateca-ca-name=\(_private_ca_name)",
	"--privateca-pool-id=\(_private_ca_pool_id)",
	"--privateca-ca-location=\(_private_ca_location)",
	"--privateca-project-id=" + #GloudProject,
]

_tink_key_uri_flags: [
	"--tink-key-uri=\(_tink_key_uri)",
	"--tink-credential-path=\(_cloud_credentials_path)",
]

_exchange_api_flags: [
	"--exchange-api-target=" + (#Target & {name: "v2alpha-public-api-server"}).target,
	"--exchange-api-cert-host=localhost",
]

example_daemon_deployment: "example_daemon_deployment": #Deployment & {
	_name:            "mp-panel-exchange-daemon"
	_image:           #ContainerRegistryPrefix + "/example-panel-exchange-daemon"
	_jvmFlags:        "-Xmx12g -Xms2g"
	_imagePullPolicy: "Always"
	_credentialsPath: _cloud_credentials_path
	_resourceConfig:  #DefaultResourceConfig
	_secretName:      _secret_name

	_args:
		_private_ca_flags +
		_exchange_api_flags +
		_tink_key_uri_flags +
		[
			"--id=\(_daemon_id)",
			"--recurring-exchange-id=\(_recurring_exchange_id)",
			"--party-type=MODEL_PROVIDER",
			"--tls-cert-file=/var/run/secrets/files/edp2_tls.pem",
			"--tls-key-file=/var/run/secrets/files/edp2_tls.key",
			"--cert-collection-file=/var/run/secrets/files/all_root_certs.pem",
			"--blob-size-limit-bytes=1000000000",
			"--storage-signing-algorithm=EC",
			"--task-timeout=24h",
			"--google-cloud-storage-bucket=\(_cloud_storage_bucket)",
			"--google-cloud-storage-project=" + #GloudProject,
			"--channel-shutdown-timeout=3s",
			"--polling-interval=1m",
			"--preprocessing-max-byte-size=1000000",
			"--preprocessing-file-count=1000",
			"--x509-common-name=SomeCommonName",
			"--x509-organization=SomeOrganization",
			"--x509-dns-name=example.com",
			"--x509-valid-days=365",
		]
}
