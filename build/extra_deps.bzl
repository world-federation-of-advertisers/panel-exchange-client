load("@wfa_common_jvm//build:common_jvm_deps.bzl", "common_jvm_deps")
load("@tink_base//:tink_base_deps_init.bzl", "tink_base_deps_init")
load("@tink_cc//:tink_cc_deps_init.bzl", "tink_cc_deps_init")

def panel_exchange_client_extra_deps():
    """Installs additional deps for panel_exchange_client."""
    common_jvm_deps()
    tink_base_deps_init()
    tink_cc_deps_init()
