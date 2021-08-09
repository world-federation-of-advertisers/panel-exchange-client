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
Step 3 of configuring WORKSPACE.
"""

load("@wfa_common_jvm//build:common_jvm_deps.bzl", "common_jvm_deps")
load("@tink_base//:tink_base_deps_init.bzl", "tink_base_deps_init")
load("@tink_cc//:tink_cc_deps_init.bzl", "tink_cc_deps_init")

def panel_exchange_client_extra_deps():
    """Installs additional deps for panel_exchange_client."""
    common_jvm_deps()
    tink_base_deps_init()
    tink_cc_deps_init()
