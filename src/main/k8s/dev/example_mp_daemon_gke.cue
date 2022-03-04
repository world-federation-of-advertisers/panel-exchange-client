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

_exchangeDaemonConfig: {
	secretName:         string @tag("secret_name")
	partyName:          string @tag("party_name")
	partyType:          "MODEL_PROVIDER"
	cloudStorageBucket: "halo-mp-test-bucket"
	clientTls: {
		certFile: "/var/run/secrets/files/mp1_tls.pem"
		keyFile:  "/var/run/secrets/files/mp1_tls.key"
	}
	tinkKeyUri: "gcp-kms://projects/halo-cmm-dev/locations/us-central1/keyRings/test-key-ring/cryptoKeys/test-aes-key"
	privateCa: {
		name:     "20220217-zm6-cbh"
		poolId:   "SomeCommonName"
		location: "us-central1"
	}
}
