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

def panel_exchange_client_deps():
    common_cpp_repositories()
    common_jvm_repositories()
    tink_base_deps()
    tink_cc_deps()
