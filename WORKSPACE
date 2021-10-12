workspace(name = "panel_exchange_client")

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

load(
    "//build:maven_deps.bzl",
    "panel_exchange_client_maven_artifacts",
    "panel_exchange_client_maven_excluded_artifacts",
    "panel_exchange_client_maven_override_targets",
)
load("@rules_jvm_external//:defs.bzl", "maven_install")

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

local_repository(
    name = "wfa_measurement_system",
    path = "/usr/local/google/home/yunyeng/principal/cross-media-measurement",
)
