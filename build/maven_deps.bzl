# Copyright 2021 The Cross-Media Measurement Authors
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

"""
Step 4 of configuring WORKSPACE: Maven.
"""

load("@wfa_common_jvm//build/maven:artifacts.bzl", "artifacts")
load("@wfa_common_jvm//build:common_jvm_maven.bzl", "COMMON_JVM_MAVEN_TARGETS", "common_jvm_maven_artifacts")

_BEAM_VERSION = "2.31.0"

# TODO: this list can likely be minimized
_ARTIFACTS = artifacts.dict_to_list({
    # Without this, we get java.lang.NoClassDefFoundError: com/google/api/gax/tracing/NoopApiTracer
    "com.google.api:gax": "2.0.0",
    "com.google.api:gax-grpc": "2.0.0",
    "com.google.apis:google-api-services-bigquery": "v2-rev20210404-1.31.0",
    "com.google.cloud:google-cloud-bigquery": "1.137.1",
    "com.google.cloud:google-cloud-nio": "0.122.0",
    "com.google.cloud:google-cloud-storage": "1.118.0",
    "com.google.code.gson:gson": "2.8.6",
    "com.google.guava:guava": "30.1.1-jre",
    "com.google.http-client:google-http-client": "1.39.2",
    "io.grpc:grpc-api": "1.37.0",
    "joda-time:joda-time": "2.10.10",
    "org.apache.beam:beam-runners-direct-java": _BEAM_VERSION,
    "org.apache.beam:beam-runners-google-cloud-dataflow-java": _BEAM_VERSION,
    "org.apache.beam:beam-sdks-java-core": _BEAM_VERSION,
    "org.apache.beam:beam-sdks-java-extensions-google-cloud-platform-core": _BEAM_VERSION,
    "org.apache.beam:beam-sdks-java-extensions-protobuf": _BEAM_VERSION,
    "org.apache.beam:beam-sdks-java-io-google-cloud-platform": _BEAM_VERSION,
    "org.apache.beam:beam-vendor-guava-26_0-jre": "0.1",
    "org.hamcrest:hamcrest": "2.2",
    "org.slf4j:slf4j-simple": "1.7.32",
})

_EXCLUDED_ARTIFACTS = [
    "org.apache.beam:beam-sdks-java-io-kafka",
]

# TODO(@efoxepstein): remove this after upgrading common-jvm version to 0.8.0.
_OVERRIDE_TARGETS = {
    "org.jetbrains.kotlin:kotlin-stdlib-common": "@com_github_jetbrains_kotlin//:kotlin-stdlib",
}

def panel_exchange_client_maven_artifacts():
    return common_jvm_maven_artifacts() + _ARTIFACTS

def panel_exchange_client_maven_override_targets():
    return dict(COMMON_JVM_MAVEN_TARGETS.items() + _OVERRIDE_TARGETS.items())

def panel_exchange_client_maven_excluded_artifacts():
    return _EXCLUDED_ARTIFACTS
