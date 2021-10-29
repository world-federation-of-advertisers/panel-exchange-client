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
        sha256 = "01cc659874288a42f65efe29477645ed57e5027747bb2ce73615ebb250667d83",
        strip_prefix = "common-jvm-0.19.0",
        url = "https://github.com/world-federation-of-advertisers/common-jvm/archive/v0.19.0.tar.gz",
    )

    # TODO: remove dependencies on wfa_measurement_system
    http_archive(
        name = "wfa_measurement_system",
        sha256 = "d8541f19d61a307814e2c7423a8f781b0f560c06c99b8ee3be6b47338a69ed8d",
        strip_prefix = "cross-media-measurement-976a969fbfd37b04576f05cca62123cd4715a81b",
        url = "https://github.com/world-federation-of-advertisers/cross-media-measurement/archive/976a969fbfd37b04576f05cca62123cd4715a81b.tar.gz",
    )

    http_archive(
        name = "wfa_measurement_proto",
        sha256 = "0b5d7b640008d85b79358be167db473e799cff3c86132be97e46333e8a70fab7",
        strip_prefix = "cross-media-measurement-api-0.10.0",
        url = "https://github.com/world-federation-of-advertisers/cross-media-measurement-api/archive/v0.10.0.tar.gz",
    )

    http_archive(
        name = "wfa_rules_swig",
        sha256 = "34c15134d7293fc38df6ed254b55ee912c7479c396178b7f6499b7e5351aeeec",
        strip_prefix = "rules_swig-653d1bdcec85a9373df69920f35961150cf4b1b6",
        url = "https://github.com/world-federation-of-advertisers/rules_swig/archive/653d1bdcec85a9373df69920f35961150cf4b1b6.tar.gz",
    )

    http_archive(
        name = "wfa_consent_signaling_client",
        sha256 = "ff82af7bc1c659d726e64c57f3ff6a16f09b4d0ee231a70ea744f8d213412998",
        strip_prefix = "consent-signaling-client-0.9.0",
        url = "https://github.com/world-federation-of-advertisers/consent-signaling-client/archive/v0.9.0.tar.gz",
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
