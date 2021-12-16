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

_cloud_storage_project:     string @tag("cloud_storage_project")
_cloud_storage_bucket:      string @tag("cloud_storage_bucket")
_container_registry:        string @tag("container_registry")
_repository_prefix:         string @tag("repository_prefix")
_container_registry_prefix: _container_registry + "/" + _repository_prefix

listObject: {
	apiVersion: "v1"
	kind:       "List"
	items:      objects
}

objects: [ for objectSet in objectSets for object in objectSet {object}]

objectSets: [ example_daemon_deployment ]

#AppName: "panel-exchange"

#Deployment: {
	_name:       string
	_replicas:   int
	_secretName: string | *""
	_image:      string
	_args: [...string]
	_ports:           [{containerPort: 8443}] | *[]
	_restartPolicy:   string | *"Always"
	_imagePullPolicy: string | *"Never"
	_system:          string
	_jvm_flags:       string | *""
	_dependencies: [...string]
	_resourceRequestCpu:    string
	_resourceLimitCpu:      string
	_resourceRequestMemory: string
	_resourceLimitMemory:   string
	apiVersion:             "apps/v1"
	kind:                   "Deployment"
	metadata: {
		name: _name + "-deployment"
		labels: {
			app:                      _name + "-app"
			"app.kubernetes.io/name": #AppName
		}
		annotations: system: _system
	}
	spec: {
		replicas: _replicas
		selector: matchLabels: app: _name + "-app"
		template: {
			metadata: labels: app: _name + "-app"
			spec: {
				containers: [{
					name:  _name + "-container"
					image: _image
					resources: requests: {
						memory: _resourceRequestMemory
						cpu:    _resourceRequestCpu
					}
					resources: limits: {
						memory: _resourceLimitMemory
						cpu:    _resourceLimitCpu
					}
					imagePullPolicy: _imagePullPolicy
					args:            _args
					ports:           _ports
					env: [{
						name:  "JAVA_TOOL_OPTIONS"
						value: _jvm_flags
					}]
					volumeMounts: [{
						name:      _name + "-files"
						mountPath: "/var/run/secrets/files"
						readOnly:  true
					}]
					readinessProbe?: {
						exec: command: [...string]
						periodSeconds: uint32
					}
				}]
				volumes: [{
					name: _name + "-files"
					secret: {
						secretName: _secretName
					}
				}]
				initContainers: [ for ds in _dependencies {
					name:  "init-\(ds)"
					image: "gcr.io/google-containers/busybox:1.27"
					command: ["sh", "-c", "until nslookup \(ds); do echo waiting for \(ds); sleep 2; done"]
				}]
				restartPolicy: _restartPolicy
			}
		}
	}
}

example_daemon_deployment: "example_daemon_deployment": #Deployment & {
  _args: [
    "--id=A",
    "--party-type=EDP",
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