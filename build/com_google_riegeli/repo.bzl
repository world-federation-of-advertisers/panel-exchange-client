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
Repository rule for Riegeli.
"""

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

def com_google_riegeli_repo():
    if "com_google_riegeli" in native.existing_rules():
        return

    http_archive(
        name = "com_google_riegeli",
        sha256 = "68adc2958d1dd54807e01155f256e79dbe0f870b2d36eed42f103aad92c59d4d",
        strip_prefix = "riegeli-7ee32d3768a6d2a86ec58a1cb1c4f08dc6744e25",
        url = "https://github.com/google/riegeli/archive/7ee32d3768a6d2a86ec58a1cb1c4f08dc6744e25.tar.gz",
    )

    http_archive(
        name = "org_brotli",
        sha256 = "fec5a1d26f3dd102c542548aaa704f655fecec3622a24ec6e97768dcb3c235ff",
        strip_prefix = "brotli-68f1b90ad0d204907beb58304d0bd06391001a4d",
        urls = [
            "https://mirror.bazel.build/github.com/google/brotli/archive/68f1b90ad0d204907beb58304d0bd06391001a4d.zip",
            "https://github.com/google/brotli/archive/68f1b90ad0d204907beb58304d0bd06391001a4d.zip",  # 2021-08-18
        ],
    )
