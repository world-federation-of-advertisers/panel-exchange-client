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

load("@rules_jvm_external//:defs.bzl", "artifact")
load(
    "@wfa_common_jvm//build:common_jvm_maven.bzl",
    "COMMON_JVM_EXCLUDED_ARTIFACTS",
    "COMMON_JVM_MAVEN_OVERRIDE_TARGETS",
    "common_jvm_maven_artifacts_dict",
)

_DEPLOY_ENV = [
    "@com_github_grpc_grpc_kotlin//stub/src/main/java/io/grpc/kotlin:stub",
    "@com_github_jetbrains_kotlin//:kotlin-reflect",
    "@com_github_jetbrains_kotlin//:kotlin-stdlib-jdk7",
    "@io_grpc_grpc_java//netty",
    "@io_grpc_grpc_java//services:health",
]

_TEST_DEPLOY_ENV = [
    "@com_github_jetbrains_kotlin//:kotlin-test",
]

_RUNTIME_DEPS = [
    artifact("org.jetbrains.kotlin:kotlin-reflect", "maven_export"),
    artifact("org.jetbrains.kotlin:kotlin-stdlib-jdk7", "maven_export"),
    artifact("io.grpc:grpc-kotlin-stub", "maven_export"),
    artifact("io.grpc:grpc-services", "maven_export"),
    artifact("io.grpc:grpc-netty", "maven_export"),
]

_TEST_RUNTIME_DEPS = [
    artifact("org.jetbrains.kotlin:kotlin-test", "maven_export"),
]

# Version compatibility info:
# * https://cloud.google.com/dataflow/docs/support/sdk-version-support-status#apache-beam-2.x-sdks
# * https://beam.apache.org/documentation/runners/flink/#flink-version-compatibility
# * https://docs.aws.amazon.com/kinesisanalytics/latest/java/earlier.html
_BEAM_VERSION = "2.38.0"

# TODO: this list can likely be minimized
_ARTIFACTS = {
    "com.google.cloud:google-cloud-security-private-ca": "2.3.1",
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
    "software.amazon.awssdk:sts": "2.17.100",
}

_EXCLUDED_ARTIFACTS = [
    "org.apache.beam:beam-sdks-java-io-kafka",
]

def panel_exchange_client_maven_artifacts():
    """Collects the Maven artifacts for panel-exchange-client.

    Returns:
        A dict of Maven artifact name to version.
    """
    common_jvm_artifacts = common_jvm_maven_artifacts_dict()

    artifacts_dict = {}
    artifacts_dict.update(common_jvm_artifacts)
    artifacts_dict.update(_ARTIFACTS)

    return artifacts_dict

def panel_exchange_client_maven_override_targets():
    return COMMON_JVM_MAVEN_OVERRIDE_TARGETS

def panel_exchange_client_maven_excluded_artifacts():
    # TODO(@efoxepstein): why does org.slf4j:slf4j-log4j12 cause build failures?
    common_jvm_exclusions = [x for x in COMMON_JVM_EXCLUDED_ARTIFACTS if x != "org.slf4j:slf4j-log4j12"]
    return _EXCLUDED_ARTIFACTS + common_jvm_exclusions

def panel_exchange_client_maven_deploy_env():
    return _DEPLOY_ENV

def panel_exchange_client_maven_runtime_deps():
    return _RUNTIME_DEPS

def panel_exchange_client_maven_test_deploy_env():
    return _TEST_DEPLOY_ENV

def panel_exchange_client_maven_test_runtime_deps():
    return _TEST_RUNTIME_DEPS
