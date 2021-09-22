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
        sha256 = "f9b2be33d3515a66e28feff14ca7405a73b4853f28912f69175bddc183805b92",
        strip_prefix = "common-jvm-0.13.0",
        url = "https://github.com/world-federation-of-advertisers/common-jvm/archive/v0.13.0.tar.gz",
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
        sha256 = "8f8f18e9438908c550d3cd2c7917a0ee76735908418c02d4548a01b85d19ad3f",
        strip_prefix = "cross-media-measurement-api-0.5.0",
        url = "https://github.com/world-federation-of-advertisers/cross-media-measurement-api/archive/v0.5.0.tar.gz",
    )

    http_archive(
        name = "wfa_rules_swig",
        sha256 = "34c15134d7293fc38df6ed254b55ee912c7479c396178b7f6499b7e5351aeeec",
        strip_prefix = "rules_swig-653d1bdcec85a9373df69920f35961150cf4b1b6",
        url = "https://github.com/world-federation-of-advertisers/rules_swig/archive/653d1bdcec85a9373df69920f35961150cf4b1b6.tar.gz",
    )

    http_archive(
        name = "wfa_consent_signaling_client",
        sha256 = "a97c4b1350eaca3e33eacf5f91611c4009c90a7abf7b2db1394ec0fa8bbaa5e0",
        strip_prefix = "consent-signaling-client-0.6.0",
        url = "https://github.com/world-federation-of-advertisers/consent-signaling-client/archive/refs/tags/v0.6.0.tar.gz",
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
