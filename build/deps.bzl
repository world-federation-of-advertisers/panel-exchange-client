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
Step 2 of configuring WORKSPACE: adds transitive deps.
"""

load("@wfa_common_cpp//build:common_cpp_repositories.bzl", "common_cpp_repositories")
load("@wfa_common_jvm//build:common_jvm_repositories.bzl", "common_jvm_repositories")
load("@tink_base//:tink_base_deps.bzl", "tink_base_deps")
load("@tink_cc//:tink_cc_deps.bzl", "tink_cc_deps")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

def private_membership_deps():
    http_archive(
        name = "com_github_google_shell",
        sha256 = "a6404a1fdc794c8e5424ed7233a925872d2abd7b08462a366f4a6485ea0747e7",
        strip_prefix = "shell-encryption-4e2c0dded993ec96eb319453e35e7d95ce689b45",
        url = "https://github.com/google/shell-encryption/archive/4e2c0dded993ec96eb319453e35e7d95ce689b45.tar.gz",
    )

    http_archive(
        name = "com_github_google_glog",
        sha256 = "f28359aeba12f30d73d9e4711ef356dc842886968112162bc73002645139c39c",
        strip_prefix = "glog-0.4.0",
        urls = ["https://github.com/google/glog/archive/v0.4.0.tar.gz"],
    )

def panel_exchange_client_deps():
    common_cpp_repositories()
    common_jvm_repositories()
    tink_base_deps()
    tink_cc_deps()
    private_membership_deps()
