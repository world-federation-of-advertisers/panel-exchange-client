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
        sha256 = "e8efc0c9f5950aff13a59f21f40ccc31c26fe40c800743f824f92df3a05588b2",
        strip_prefix = "common-cpp-0.5.0",
        url = "https://github.com/world-federation-of-advertisers/common-cpp/archive/v0.5.0.tar.gz",
    )

    http_archive(
        name = "wfa_common_jvm",
        sha256 = "12819b095363d8294bd01c0e9c2cf847b21f895e96923b8a8c07836c8cd2c042",
        strip_prefix = "common-jvm-0.20.1",
        url = "https://github.com/world-federation-of-advertisers/common-jvm/archive/v0.20.1.tar.gz",
    )

    # TODO: remove dependencies on wfa_measurement_system
    http_archive(
        name = "wfa_measurement_system",
        sha256 = "b723d0c3682854fad8fd0937bd830c4a9c2bf60ad0e0099c4fd6abb9046997e1",
        strip_prefix = "cross-media-measurement-2449b39c5ce3ed14944437bbe04b988c7a146902",
        url = "https://github.com/world-federation-of-advertisers/cross-media-measurement/archive/2449b39c5ce3ed14944437bbe04b988c7a146902.tar.gz",
    )

    http_archive(
        name = "wfa_measurement_proto",
        sha256 = "611bbc8c653868c1dbc973a520a192d8ac1678375167181354fc9b1bc8e3a3ea",
        strip_prefix = "cross-media-measurement-api-0.14.0",
        url = "https://github.com/world-federation-of-advertisers/cross-media-measurement-api/archive/v0.14.0.tar.gz",
    )

    http_archive(
        name = "wfa_rules_swig",
        sha256 = "34c15134d7293fc38df6ed254b55ee912c7479c396178b7f6499b7e5351aeeec",
        strip_prefix = "rules_swig-653d1bdcec85a9373df69920f35961150cf4b1b6",
        url = "https://github.com/world-federation-of-advertisers/rules_swig/archive/653d1bdcec85a9373df69920f35961150cf4b1b6.tar.gz",
    )

    http_archive(
        name = "wfa_consent_signaling_client",
        sha256 = "59079953c01f2f9e2f22c0c2e2e01cfbb5da0a7b9348980e18827bf1947b21f5",
        strip_prefix = "consent-signaling-client-0.10.0",
        url = "https://github.com/world-federation-of-advertisers/consent-signaling-client/archive/v0.10.0.tar.gz",
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
        sha256 = "8ef6b90df3c8abd300fe254bba73c6c0314d2c64127e369c04d7af1fb99a7115",
        strip_prefix = "private-membership-2216c76f40c1c3d03e8cf7d1ad088af184ff6e4c",
        url = "https://github.com/google/private-membership/archive/2216c76f40c1c3d03e8cf7d1ad088af184ff6e4c.zip",
    )
