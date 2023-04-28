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
        sha256 = "be7564a574c60176dc63e48daba6135263779deb301baacb77d2328bdaf38d3d",
        strip_prefix = "common-cpp-0.9.0",
        url = "https://github.com/world-federation-of-advertisers/common-cpp/archive/refs/tags/v0.9.0.tar.gz",
    )

    http_archive(
        name = "wfa_common_jvm",
        sha256 = "0b4a8eb501f0abca95d994bd199cda845a9238f57b34be74e7e54220ec66a48b",
        strip_prefix = "common-jvm-0.54.0",
        url = "https://github.com/world-federation-of-advertisers/common-jvm/archive/refs/tags/v0.54.0.tar.gz",
    )

    # TODO: remove dependencies on wfa_measurement_system
    http_archive(
        name = "wfa_measurement_system",
        sha256 = "b7fec8fa6cecfdd149364f4cd923cbfb324d46b2a4b0dbf4639fd109c2f7322d",
        strip_prefix = "cross-media-measurement-ba3ebc3c2c9e3f4febc446b94cc2c7516a8adcd8",
        url = "https://github.com/world-federation-of-advertisers/cross-media-measurement/archive/10c44b1af7c13481258eb8d9e5c1df4178b4be62.tar.gz",
    )

    http_archive(
        name = "wfa_measurement_proto",
        sha256 = "333ec3153cfe20d9f0ceeb9c73b0d11daa9f0b61382596de76d7090511bc591a",
        strip_prefix = "cross-media-measurement-api-0.28.1",
        url = "https://github.com/world-federation-of-advertisers/cross-media-measurement-api/archive/v0.28.1.tar.gz",
    )

    http_archive(
        name = "wfa_rules_swig",
        sha256 = "34c15134d7293fc38df6ed254b55ee912c7479c396178b7f6499b7e5351aeeec",
        strip_prefix = "rules_swig-653d1bdcec85a9373df69920f35961150cf4b1b6",
        url = "https://github.com/world-federation-of-advertisers/rules_swig/archive/653d1bdcec85a9373df69920f35961150cf4b1b6.tar.gz",
    )

    http_archive(
        name = "wfa_rules_cue",
        sha256 = "652379dec5174ed7fa8fe4223d0adf9a1d610ff0aa02e1bd1e74f79834b526a6",
        strip_prefix = "rules_cue-0.2.0",
        url = "https://github.com/world-federation-of-advertisers/rules_cue/archive/v0.2.0.tar.gz",
    )

    http_archive(
        name = "wfa_consent_signaling_client",
        sha256 = "99fde5608b79ff12a2a466cdd213e1535c62f80a96035006433ae9ba5a4a4d21",
        strip_prefix = "consent-signaling-client-0.15.0",
        url = "https://github.com/world-federation-of-advertisers/consent-signaling-client/archive/refs/tags/v0.15.0.tar.gz",
    )

    http_archive(
        name = "wfa_virtual_people_common",
        sha256 = "bcc79b67195e0f144d7b2f21788da4b9d43fb1c7cd129c76941ec81c9fb4ac5a",
        strip_prefix = "virtual-people-common-0.1.0",
        url = "https://github.com/world-federation-of-advertisers/virtual-people-common/archive/v0.1.0.tar.gz",
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
