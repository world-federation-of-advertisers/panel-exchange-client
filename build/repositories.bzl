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
Step 1 of configuring WORKSPACE: adds direct deps.
"""

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
load("//build/com_google_riegeli:repo.bzl", "com_google_riegeli_repo")

def panel_exchange_client_repositories():
    """Imports all direct dependencies for panel_exchange_client."""

    com_google_riegeli_repo()

    http_archive(
        name = "wfa_common_cpp",
        sha256 = "aedf667f2b2a42afa251ce5f0032fe6fbc4298ae9a389872ed43deb1e607ac2d",
        strip_prefix = "common-cpp-0.4.1",
        url = "https://github.com/world-federation-of-advertisers/common-cpp/archive/v0.4.1.tar.gz",
    )

    http_archive(
        name = "wfa_common_jvm",
        sha256 = "81fe8d280134427b0911bb396cdd333be83fe4d4f5c5aa6afe6502f55e7a2ee8",
        strip_prefix = "common-jvm-0.12.0",
        url = "https://github.com/world-federation-of-advertisers/common-jvm/archive/v0.12.0.tar.gz",
    )

    # TODO: remove dependencies on wfa_measurement_system
    http_archive(
        name = "wfa_measurement_system",
        sha256 = "6e76b7ad3d7f4b005a091343ec8845fac0d1d6c5f24c6ac25d1f27b41a72585c",
        strip_prefix = "cross-media-measurement-3fd2a7fde637f633588a64e39620f47bddaeb004",
        url = "https://github.com/world-federation-of-advertisers/cross-media-measurement/archive/3fd2a7fde637f633588a64e39620f47bddaeb004.tar.gz",
    )

    http_archive(
        name = "wfa_measurement_proto",
        sha256 = "94b6ed87c4c9917da80fc4f5803b2c62a93767f433bfd7f25e5c6c9dc355aa38",
        strip_prefix = "cross-media-measurement-api-640987b5196e26fe717a47875f603360d6c11346",
        url = "https://github.com/world-federation-of-advertisers/cross-media-measurement-api/archive/640987b5196e26fe717a47875f603360d6c11346.tar.gz",
    )

    http_archive(
        name = "wfa_rules_swig",
        sha256 = "34c15134d7293fc38df6ed254b55ee912c7479c396178b7f6499b7e5351aeeec",
        strip_prefix = "rules_swig-653d1bdcec85a9373df69920f35961150cf4b1b6",
        url = "https://github.com/world-federation-of-advertisers/rules_swig/archive/653d1bdcec85a9373df69920f35961150cf4b1b6.tar.gz",
    )

    http_archive(
        name = "tink_base",
        sha256 = "005e6c49b2b2df8a7dc670471ee45b6e09092bb05046eea358cd47f2703359c4",
        strip_prefix = "tink-7c93a224b8fa6a3babfaf71c18c5610052dcbd61/",
        url = "https://github.com/google/tink/archive/7c93a224b8fa6a3babfaf71c18c5610052dcbd61.zip",
    )

    http_archive(
        name = "tink_cc",
        sha256 = "005e6c49b2b2df8a7dc670471ee45b6e09092bb05046eea358cd47f2703359c4",
        strip_prefix = "tink-7c93a224b8fa6a3babfaf71c18c5610052dcbd61/cc",
        url = "https://github.com/google/tink/archive/7c93a224b8fa6a3babfaf71c18c5610052dcbd61.zip",
    )

    http_archive(
        name = "private_membership",
        sha256 = "5267803c14f1468947825fa1d1e6b810c277e13115877d2a86ad8a5028c99196",
        strip_prefix = "private-membership-e111e973646aec4b7735d439082a030849967dc3",
        url = "https://github.com/google/private-membership/archive/e111e973646aec4b7735d439082a030849967dc3.zip",
    )
