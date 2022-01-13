workspace(name = "panel_exchange_client")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

RULES_JVM_EXTERNAL_TAG = "4.2"

RULES_JVM_EXTERNAL_SHA = "cd1a77b7b02e8e008439ca76fd34f5b07aecb8c752961f9640dea15e9e5ba1ca"

http_archive(
    name = "rules_jvm_external",
    sha256 = RULES_JVM_EXTERNAL_SHA,
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
)

load("//build:repositories.bzl", "panel_exchange_client_repositories")

panel_exchange_client_repositories()

# TODO: it should not be necessary to run `switched_rules_by_language` here.
# This is done by `common_jvm` set-up -- but apparently it happens too late
# there.
load("@wfa_common_jvm//build/com_google_googleapis:repo.bzl", "com_google_googleapis_repo")

com_google_googleapis_repo()

load("@com_google_googleapis//:repository_rules.bzl", "switched_rules_by_language")

switched_rules_by_language(
    name = "com_google_googleapis_imports",
    java = True,
)

load("//build:deps.bzl", "panel_exchange_client_deps")

panel_exchange_client_deps()

load("//build:extra_deps.bzl", "panel_exchange_client_extra_deps")

panel_exchange_client_extra_deps()

load("@rules_jvm_external//:repositories.bzl", "rules_jvm_external_deps")

rules_jvm_external_deps()

load("@rules_jvm_external//:setup.bzl", "rules_jvm_external_setup")

rules_jvm_external_setup()

load("@rules_jvm_external//:defs.bzl", "maven_install")
load(
    "//build:maven_deps.bzl",
    "panel_exchange_client_maven_artifacts",
    "panel_exchange_client_maven_excluded_artifacts",
    "panel_exchange_client_maven_override_targets",
)

maven_install(
    artifacts = panel_exchange_client_maven_artifacts(),
    excluded_artifacts = panel_exchange_client_maven_excluded_artifacts(),
    fetch_sources = True,
    generate_compat_repositories = True,
    override_targets = panel_exchange_client_maven_override_targets(),
    repositories = [
        "https://repo.maven.apache.org/maven2/",
    ],
)

load("@wfa_common_jvm//build:common_jvm_extra_deps.bzl", "common_jvm_extra_deps")

common_jvm_extra_deps()
