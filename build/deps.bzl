load("@wfa_common_cpp//build:common_cpp_repositories.bzl", "common_cpp_repositories")
load("@wfa_common_jvm//build:common_jvm_repositories.bzl", "common_jvm_deps_repositories")
load("@tink_base//:tink_base_deps.bzl", "tink_base_deps")
load("@tink_cc//:tink_cc_deps.bzl", "tink_cc_deps")

def panel_exchange_client_deps():
    common_cpp_repositories()
    common_jvm_deps_repositories()
    tink_base_deps()
    tink_cc_deps()
