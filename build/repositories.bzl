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

    # TODO: remove this dependency once downstream dependencies are fixed.
    http_archive(
        name = "io_bazel_rules_go",
        sha256 = "2b1641428dff9018f9e85c0384f03ec6c10660d935b750e3fa1492a281a53b0f",
        url = "https://github.com/bazelbuild/rules_go/releases/download/v0.29.0/rules_go-v0.29.0.zip",
    )

    # TODO: remove this dependency once downstream dependencies are fixed.
    http_archive(
        name = "bazel_gazelle",
        sha256 = "de69a09dc70417580aabf20a28619bb3ef60d038470c7cf8442fafcf627c21cb",
        url = "https://github.com/bazelbuild/bazel-gazelle/releases/download/v0.24.0/bazel-gazelle-v0.24.0.tar.gz",
    )

    http_archive(
        name = "wfa_common_cpp",
        sha256 = "e8efc0c9f5950aff13a59f21f40ccc31c26fe40c800743f824f92df3a05588b2",
        strip_prefix = "common-cpp-0.5.0",
        url = "https://github.com/world-federation-of-advertisers/common-cpp/archive/v0.5.0.tar.gz",
    )

    http_archive(
        name = "wfa_common_jvm",
        sha256 = "3438df3499a40fab3b684fd6d437a7bc7633d5e4f359e74de81c3e18195f46f1",
        strip_prefix = "common-jvm-0.32.0",
        url = "https://github.com/world-federation-of-advertisers/common-jvm/archive/refs/tags/v0.32.0.tar.gz",
    )

    # TODO: remove dependencies on wfa_measurement_system
    http_archive(
        name = "wfa_measurement_system",
        sha256 = "b0b0841355c4329da693d3a2bdc4f7a62cf41c952b1ed25d0596c6b3f24ca069",
        strip_prefix = "cross-media-measurement-42b50e6bd158bc920f2575aaa17db9b7cf83b4a6",
        url = "https://github.com/world-federation-of-advertisers/cross-media-measurement/archive/42b50e6bd158bc920f2575aaa17db9b7cf83b4a6.tar.gz",
    )

    http_archive(
        name = "wfa_measurement_proto",
        sha256 = "6599d5c3ddab99a28065299a6e9cb5e11d42c31e0692a529bf5a32d36d81de15",
        strip_prefix = "cross-media-measurement-api-0.15.6",
        url = "https://github.com/world-federation-of-advertisers/cross-media-measurement-api/archive/v0.15.6.tar.gz",
    )

    http_archive(
        name = "wfa_rules_swig",
        sha256 = "34c15134d7293fc38df6ed254b55ee912c7479c396178b7f6499b7e5351aeeec",
        strip_prefix = "rules_swig-653d1bdcec85a9373df69920f35961150cf4b1b6",
        url = "https://github.com/world-federation-of-advertisers/rules_swig/archive/653d1bdcec85a9373df69920f35961150cf4b1b6.tar.gz",
    )

    http_archive(
        name = "wfa_rules_cue",
        sha256 = "62def6a4dc401fd1549e44e2a4e2ae73cf75e6870025329bc78a0150d9a2594a",
        strip_prefix = "rules_cue-0.1.0",
        url = "https://github.com/world-federation-of-advertisers/rules_cue/archive/v0.1.0.tar.gz",
    )

    http_archive(
        name = "wfa_consent_signaling_client",
        sha256 = "6f92694715ec6d03a9cb5288db2ad167cc69d1a9331ca18fd7b7cf584e34b12c",
        strip_prefix = "consent-signaling-client-0.11.0",
        url = "https://github.com/world-federation-of-advertisers/consent-signaling-client/archive/v0.11.0.tar.gz",
    )

    http_archive(
            name = "wfa_virtual_people_common",
            sha256 = "bcc79b67195e0f144d7b2f21788da4b9d43fb1c7cd129c76941ec81c9fb4ac5a",
            strip_prefix = "virtual-people-common-0.1.0",
            url = "https://github.com/world-federation-of-advertisers/virtual-people-common/archive/v0.1.0.tar.gz",
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
        sha256 = "b1e0e7f74f4da09a6011c6fa91d7b968cdff6bb571712490dae427704b2af14c",
        strip_prefix = "private-membership-84e45669f7357bffcdafbc1b0cc26e72512808ce",
        url = "https://github.com/google/private-membership/archive/84e45669f7357bffcdafbc1b0cc26e72512808ce.zip",
    )

    http_archive(
        name = "rules_pkg",
        urls = [
            "https://mirror.bazel.build/github.com/bazelbuild/rules_pkg/releases/download/0.6.0/rules_pkg-0.6.0.tar.gz",
            "https://github.com/bazelbuild/rules_pkg/releases/download/0.6.0/rules_pkg-0.6.0.tar.gz",
        ],
        sha256 = "62eeb544ff1ef41d786e329e1536c1d541bb9bcad27ae984d57f18f314018e66",
    )
