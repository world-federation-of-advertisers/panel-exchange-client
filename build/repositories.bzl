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

_TINK = struct(
    # TODO: Use version once there's a release that contains AesSivBoringSsl.
    commit = "0f65dc5d079fb3107c71908734a082079e98ae45",
    sha256 = "0b8bbaffee4903faea66dbad76f8eb6d0eea3f94367807bebc49180f9f417031",
    url_template = "https://github.com/google/tink/archive/{commit}.tar.gz",
)

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
        sha256 = "42066b67f54b2d4162dfab6dfd4a9747d9f86eb2a8710f678f37349b1005f3fc",
        strip_prefix = "common-jvm-31e7100184ea500c31dca2e2aafb46b867830166",
        # TODO(world-federation-of-advertisers/common-jvm#114): Use version.
        url = "https://github.com/world-federation-of-advertisers/common-jvm/archive/31e7100184ea500c31dca2e2aafb46b867830166.tar.gz",
    )

    # TODO: remove dependencies on wfa_measurement_system
    http_archive(
        name = "wfa_measurement_system",
        sha256 = "a8e111f8fd68fc355da4d87405a5f09b2b2b470b471003ec50e531d62751d754",
        strip_prefix = "cross-media-measurement-b1586f8bdd8435c8b259a5b25ccefd9bb333fba4",
        url = "https://github.com/world-federation-of-advertisers/cross-media-measurement/archive/b1586f8bdd8435c8b259a5b25ccefd9bb333fba4.tar.gz",
    )

    http_archive(
        name = "wfa_measurement_proto",
        sha256 = "da28ccac88a12b3b75b974b92604b8e332b8bc91cd276afab1ee41415fa320a3",
        strip_prefix = "cross-media-measurement-api-0.22.2",
        url = "https://github.com/world-federation-of-advertisers/cross-media-measurement-api/archive/v0.22.2.tar.gz",
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
        sha256 = "b907c0dd4f6efbe4f6db3f34efeca0f1763d3cc674c37cbfebac1ee2a80c86f5",
        strip_prefix = "consent-signaling-client-0.12.0",
        url = "https://github.com/world-federation-of-advertisers/consent-signaling-client/archive/refs/tags/v0.12.0.tar.gz",
    )

    http_archive(
        name = "wfa_virtual_people_common",
        sha256 = "bcc79b67195e0f144d7b2f21788da4b9d43fb1c7cd129c76941ec81c9fb4ac5a",
        strip_prefix = "virtual-people-common-0.1.0",
        url = "https://github.com/world-federation-of-advertisers/virtual-people-common/archive/v0.1.0.tar.gz",
    )

    http_archive(
        name = "tink_base",
        sha256 = _TINK.sha256,
        strip_prefix = "tink-{commit}".format(commit = _TINK.commit),
        url = _TINK.url_template.format(commit = _TINK.commit),
    )

    http_archive(
        name = "tink_cc",
        sha256 = _TINK.sha256,
        strip_prefix = "tink-{commit}/cc".format(commit = _TINK.commit),
        url = _TINK.url_template.format(commit = _TINK.commit),
    )

    http_archive(
        name = "tink_java",
        sha256 = _TINK.sha256,
        strip_prefix = "tink-{commit}/java_src".format(commit = _TINK.commit),
        url = _TINK.url_template.format(commit = _TINK.commit),
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
