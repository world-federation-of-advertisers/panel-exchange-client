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

package k8s

_daemon_id:                 string @tag("daemon_id")
_daemon_party_type:         string @tag("daemon_party_type")
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
	_image:      string
	_args: [...string]
	_ports:           [{containerPort: 8443}] | *[]
	_restartPolicy:   string | *"Always"
	_imagePullPolicy: string | *"Never"
	_jvm_flags:       string | *""
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
				restartPolicy: _restartPolicy
			}
		}
	}
}