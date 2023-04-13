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
        sha256 = "60e9c808d55d14be65347cab008b8bd4f8e2dd8186141609995333bc75fc08ce",
        strip_prefix = "common-cpp-0.8.0",
        url = "https://github.com/world-federation-of-advertisers/common-cpp/archive/refs/tags/v0.8.0.tar.gz",
    )

    http_archive(
        name = "wfa_common_jvm",
        sha256 = "9505024528afc9e7a9e126a297458fa4503a33ff21c55bac58e5184385f492e2",
        strip_prefix = "common-jvm-0.35.0",
        url = "https://github.com/world-federation-of-advertisers/common-jvm/archive/refs/tags/v0.35.0.tar.gz",
    )

    # TODO: remove dependencies on wfa_measurement_system
    http_archive(
        name = "wfa_measurement_system",
        sha256 = "21bb52836db72ee23a6c8caca4970b8f1e24108b7734f5c18d36c22526449d2a",
        strip_prefix = "cross-media-measurement-bc97937a40037fe9c5320850de7b100e96de807b",
        url = "https://github.com/world-federation-of-advertisers/cross-media-measurement/archive/bc97937a40037fe9c5320850de7b100e96de807b.tar.gz",
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
        sha256 = "0302c92075d991e89ed29a19c946abb3abc634430bb3cde9b77774b49079354e",
        strip_prefix = "virtual-people-common-0.2.2",
        url = "https://github.com/world-federation-of-advertisers/virtual-people-common/archive/v0.2.2.tar.gz",
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
