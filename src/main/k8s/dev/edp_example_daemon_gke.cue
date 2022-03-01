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

_container_registry:     string @tag("container_registry")
_image_repo_prefix:      string @tag("image_repo_prefix")
_secret_name:            string @tag("secret_name")
_daemon_id:              string @tag("daemon_id")
_recurring_exchange_id:  string @tag("recurring_exchange_id")

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
	"--privateca-ca-name=20220217-zm6-cbh",
	"--privateca-pool-id=SomeCommonName",
	"--privateca-ca-location=us-central1",
	"--privateca-project-id=" + #GloudProject,
]

_tink_key_uri_flags: [
	"--tink-key-uri=gcp-kms://projects/halo-cmm-dev/locations/us-central1/keyRings/test-key-ring/cryptoKeys/test-aes-key"
]

_exchange_api_flags: [
	"--exchange-api-target=" + (#Target & {name: "v2alpha-public-api-server"}).target,
	"--exchange-api-cert-host=localhost",
]

example_daemon_deployment: "example_daemon_deployment": #Deployment & {
	_name:            "edp-panel-exchange-daemon"
	_image:           #ContainerRegistryPrefix + "/example-panel-exchange-daemon"
	_jvmFlags:        "-Xmx12g -Xms2g"
	_imagePullPolicy: "Always"
	_credentialsPath: "/var/run/secrets/files/halo-cmm-dev-creds.json"
	_resourceConfig:  #DefaultResourceConfig
	_secretName:      _secret_name

	_args:
		_exchange_api_flags +
		_tink_key_uri_flags +
		_private_ca_flags +
		[
			"--id=\(_daemon_id)",
			"--recurring-exchange-id=\(_recurring_exchange_id)",
			"--party-type=DATA_PROVIDER",
			"--tls-cert-file=/var/run/secrets/files/edp4_tls.pem",
			"--tls-key-file=/var/run/secrets/files/edp4_tls.key",
			"--cert-collection-file=/var/run/secrets/files/all_root_certs.pem",
			"--blob-size-limit-bytes=1000000000",
			"--storage-signing-algorithm=EC",
			"--task-timeout=24h",
			"--google-cloud-storage-bucket=halo-panel-edp-dev-bucket",
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
