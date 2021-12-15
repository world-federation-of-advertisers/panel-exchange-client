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
load(
    "@wfa_common_jvm//build:common_jvm_maven.bzl",
    "COMMON_JVM_EXCLUDED_ARTIFACTS",
    "COMMON_JVM_MAVEN_OVERRIDE_TARGETS",
    "common_jvm_maven_artifacts",
)

_BEAM_VERSION = "2.34.0"

# TODO: this list can likely be minimized
_ARTIFACTS = artifacts.dict_to_list({
    # Without this, we get java.lang.NoClassDefFoundError: com/google/api/gax/tracing/NoopApiTracer
    "com.google.api:api-common": "2.1.1",
    "com.google.api:gax": "2.7.0",
    "com.google.api:gax-grpc": "2.7.0",
    "com.google.api.grpc:proto-google-cloud-security-private-ca-v1": "2.2.0",
    "com.google.cloud:google-cloud-bigquery": "2.4.1",
    "com.google.cloud:google-cloud-bigquerystorage": "2.6.3",
    "com.google.cloud:google-cloud-core": "2.3.3",
    "com.google.cloud:google-cloud-nio": "0.123.10",
    "com.google.cloud:google-cloud-security-private-ca": "2.2.0",
    "com.google.cloud:google-cloud-storage": "2.2.1",
    "com.google.code.gson:gson": "2.8.9",
    "com.google.crypto.tink:tink": "1.6.0",
    "com.google.guava:guava": "31.0.1-jre",
    "com.google.http-client:google-http-client": "1.40.1",
    "io.grpc:grpc-api": "1.42.1",
    "io.grpc:grpc-core": "1.42.1",
    "io.grpc:grpc-netty": "1.42.1",
    "io.grpc:grpc-netty-shaded": "1.42.1",
    "joda-time:joda-time": "2.10.13",
    "org.apache.beam:beam-runners-direct-java": _BEAM_VERSION,
    "org.apache.beam:beam-runners-google-cloud-dataflow-java": _BEAM_VERSION,
    "org.apache.beam:beam-sdks-java-core": _BEAM_VERSION,
    "org.apache.beam:beam-sdks-java-extensions-google-cloud-platform-core": _BEAM_VERSION,
    "org.apache.beam:beam-sdks-java-extensions-protobuf": _BEAM_VERSION,
    "org.apache.beam:beam-sdks-java-io-google-cloud-platform": _BEAM_VERSION,
    "org.apache.beam:beam-vendor-guava-26_0-jre": "0.1",
    "org.hamcrest:hamcrest": "2.2",
    "org.slf4j:slf4j-simple": "1.7.32",
    "software.amazon.awssdk:utils": "2.17.100",
})

_EXCLUDED_ARTIFACTS = [
    "org.apache.beam:beam-sdks-java-io-kafka",
]

def panel_exchange_client_maven_artifacts():
    return common_jvm_maven_artifacts() + _ARTIFACTS

def panel_exchange_client_maven_override_targets():
    return COMMON_JVM_MAVEN_OVERRIDE_TARGETS

def panel_exchange_client_maven_excluded_artifacts():
    # TODO(@efoxepstein): why does org.slf4j:slf4j-log4j12 cause build failures?
    common_jvm_exclusions = [x for x in COMMON_JVM_EXCLUDED_ARTIFACTS if x != "org.slf4j:slf4j-log4j12"]
    return _EXCLUDED_ARTIFACTS + common_jvm_exclusions
