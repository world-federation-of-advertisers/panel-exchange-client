#!/usr/bin/env bash
# Copyright 2022 The Cross-Media Measurement Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -e

echo "||| putting together secrets"
cat src/main/k8s/testing/secretfiles/*_root.pem > src/main/k8s/testing/secretfiles/trusted_certs.pem
cat <<EOT > src/main/k8s/testing/secretfiles/kustomization.yaml
secretGenerator:
- name: certs-and-configs
  Files:
  - trusted_certs.pem
  - mp2_tls.pem
  - mp2_tls.key
EOT

echo "||| applying secrets with command:"
echo "    kubectl apply -k src/main/k8s/testing/secretfiles"
kubectl apply -k src/main/k8s/testing/secretfiles

echo "||| applying kustomization with command:"
echo "    bazel run //src/main/k8s/testing/secretfiles:apply_kustomization"
str=$(bazel run //src/main/k8s/testing/secretfiles:apply_kustomization)
regex="(certs-and-configs-\S*)"
[[ $str =~ $regex ]]
secret_name=${BASH_REMATCH[0]}
echo "||| applying secret: $secret_name"

echo "||| building manifest"
bazel build //src/main/k8s/dev:example_mp_daemon_aws --define=mp_name=modelProviders/Wt5MH8egH4w --define=mp_k8s_secret_name=$secret_name
echo "||| applying manifest"
kubectl apply -f bazel-bin/src/main/k8s/dev/example_mp_daemon_aws.yaml
echo "||| restart deployment"
kubectl rollout restart deployment/example-panel-exchange-daemon-deployment

#kubectl logs -f deployment/example-panel-exchange-daemon-deployment
