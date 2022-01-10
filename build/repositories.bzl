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
        sha256 = "233b7e0443c8e986237f3879e65226fd8263c551bcc5d011dd435900447a5711",
        strip_prefix = "common-jvm-0.24.0",
        url = "https://github.com/world-federation-of-advertisers/common-jvm/archive/v0.24.0.tar.gz",
    )

    # TODO: remove dependencies on wfa_measurement_system
    http_archive(
        name = "wfa_measurement_system",
        sha256 = "46345624a7095ed0af2c194e0ea61ba53832484bc38e963081b72b1c123eda39",
        strip_prefix = "cross-media-measurement-b725633b16081a11294cbb63127081041173d0a9",
        url = "https://github.com/world-federation-of-advertisers/cross-media-measurement/archive/b725633b16081a11294cbb63127081041173d0a9.tar.gz",
    )

    # TODO: Update to latest release before merging
    http_archive(
        name = "wfa_measurement_proto",
        sha256 = "3591cd775836e88c62df70b4a413830e049dac5c0dcd9729f1942423232436f7",
        strip_prefix = "cross-media-measurement-api-68ab970709efa72620607fc2e291f8b68d7713f1",
        url = "https://github.com/world-federation-of-advertisers/cross-media-measurement-api/archive/68ab970709efa72620607fc2e291f8b68d7713f1.tar.gz",
    )

    http_archive(
        name = "wfa_rules_swig",
        sha256 = "34c15134d7293fc38df6ed254b55ee912c7479c396178b7f6499b7e5351aeeec",
        strip_prefix = "rules_swig-653d1bdcec85a9373df69920f35961150cf4b1b6",
        url = "https://github.com/world-federation-of-advertisers/rules_swig/archive/653d1bdcec85a9373df69920f35961150cf4b1b6.tar.gz",
    )

    http_archive(
        name = "wfa_consent_signaling_client",
        sha256 = "6f92694715ec6d03a9cb5288db2ad167cc69d1a9331ca18fd7b7cf584e34b12c",
        strip_prefix = "consent-signaling-client-0.11.0",
        url = "https://github.com/world-federation-of-advertisers/consent-signaling-client/archive/v0.11.0.tar.gz",
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
