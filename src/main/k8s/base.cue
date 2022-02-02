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

package k8s

import (
	"strings"
)

listObject: {
	apiVersion: "v1"
	kind:       "List"
	items:      objects
}

objects: [ for objectSet in objectSets for object in objectSet {object}]

objectSets: [ example_daemon_deployment ]

#AppName: "panel-exchange"

#Target: {
	name:   string
	_caps:  strings.Replace(strings.ToUpper(name), "-", "_", -1)
	target: "$(" + _caps + "_SERVICE_HOST):$(" + _caps + "_SERVICE_PORT)"
}

#ResourceConfig: {
	replicas:              int
	resourceRequestCpu:    string
	resourceLimitCpu:      string
	resourceRequestMemory: string
	resourceLimitMemory:   string
}

#Deployment: {
	_name:                  string
	_image:                 string
	_args:                  [...string]
	_ports:                 [{containerPort: 8443}] | *[]
	_restartPolicy:         string | *"Always"
	_imagePullPolicy:       string | *"Never"
	_jvmFlags:              string | *""
	_resourceConfig:        #ResourceConfig
	_secretName:            string | *null
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
		replicas: _resourceConfig.replicas
		selector: matchLabels: app: _name + "-app"
		template: {
			metadata: labels: app: _name + "-app"
			spec: {
				containers: [{
					name:  _name + "-container"
					image: _image
					resources: requests: {
						memory: _resourceConfig.resourceRequestMemory
						cpu:    _resourceConfig.resourceRequestCpu
					}
					resources: limits: {
						memory: _resourceConfig.resourceLimitMemory
						cpu:    _resourceConfig.resourceLimitCpu
					}
					imagePullPolicy: _imagePullPolicy
					args:            _args
					ports:           _ports
					env: [{
						name:  "JAVA_TOOL_OPTIONS"
						value: _jvmFlags
					}]
					if _secretName != null {
					volumeMounts: [{
						name:      _name + "-files"
						mountPath: "/var/run/secrets/files"
						readOnly:  true
					}]
				}
				}]
				if _secretName != null {
				volumes: [{
					name: _name + "-files"
					secret: {
						secretName: _secretName
					}
				}]
			}
				restartPolicy: _restartPolicy
			}
		}
	}
}