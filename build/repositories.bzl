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
        sha256 = "82fde6e22865aa00334aded04611ad309ae60d49f04de9860f2e52665df7113e",
        strip_prefix = "common-jvm-0.14.0",
        url = "https://github.com/world-federation-of-advertisers/common-jvm/archive/v0.14.0.tar.gz",
    )

    # TODO: remove dependencies on wfa_measurement_system
    http_archive(
        name = "wfa_measurement_system",
        sha256 = "9d531684e3da0fd6a15260c591474802305748f42cb5af32893f84f516a19024",
        strip_prefix = "cross-media-measurement-37267399a1563ded00375a7562bfcf8c44520531",
        url = "https://github.com/world-federation-of-advertisers/cross-media-measurement/archive/37267399a1563ded00375a7562bfcf8c44520531.tar.gz",
    )

    http_archive(
        name = "wfa_measurement_proto",
        sha256 = "8122c14c85cbb44ff918bb9f4d3ad25e9560726bc52a9599a597edac4c77eef2",
        strip_prefix = "cross-media-measurement-api-0.7.3",
        url = "https://github.com/world-federation-of-advertisers/cross-media-measurement-api/archive/v0.7.3.tar.gz",
    )

    http_archive(
        name = "wfa_rules_swig",
        sha256 = "34c15134d7293fc38df6ed254b55ee912c7479c396178b7f6499b7e5351aeeec",
        strip_prefix = "rules_swig-653d1bdcec85a9373df69920f35961150cf4b1b6",
        url = "https://github.com/world-federation-of-advertisers/rules_swig/archive/653d1bdcec85a9373df69920f35961150cf4b1b6.tar.gz",
    )

    http_archive(
        name = "wfa_consent_signaling_client",
        sha256 = "b46a870d5be459f8db2f136e2685d9310580bf0c4a0c9e1fef5fb61d8f8beb5a",
        strip_prefix = "consent-signaling-client-0.8.1",
        url = "https://github.com/world-federation-of-advertisers/consent-signaling-client/archive/v0.8.1.tar.gz",
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
