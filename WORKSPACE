workspace(name = "panel_exchange_client")

load("//build:repositories.bzl", "panel_exchange_client_repositories")

panel_exchange_client_repositories()

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
load("@rules_jvm_external//:repositories.bzl", "rules_jvm_external_deps")

rules_jvm_external_deps()

load("@rules_jvm_external//:setup.bzl", "rules_jvm_external_setup")

rules_jvm_external_setup()

load(
    "@wfa_common_jvm//build:versions.bzl",
    "GRPC_JAVA_VERSION",
    "KOTLIN_RELEASE_VERSION",
)
load("@wfa_common_jvm//build/maven:artifacts.bzl", "artifacts")
load("@rules_jvm_external//:defs.bzl", "maven_install")

maven_install(
    artifacts = artifacts.dict_to_list(panel_exchange_client_maven_artifacts()),
    excluded_artifacts = panel_exchange_client_maven_excluded_artifacts(),
    fetch_sources = True,
    generate_compat_repositories = True,
    override_targets = panel_exchange_client_maven_override_targets(),
    repositories = [
        "https://repo.maven.apache.org/maven2/",
    ],
)

maven_install(
    name = "maven_export",
    artifacts = [
        "io.grpc:grpc-kotlin-stub:1.2.0",
        "io.grpc:grpc-netty:" + GRPC_JAVA_VERSION,
        "io.grpc:grpc-services:" + GRPC_JAVA_VERSION,
        "org.jetbrains.kotlin:kotlin-reflect:" + KOTLIN_RELEASE_VERSION,
        "org.jetbrains.kotlin:kotlin-stdlib-jdk7:" + KOTLIN_RELEASE_VERSION,
        "org.jetbrains.kotlin:kotlin-test:" + KOTLIN_RELEASE_VERSION,
    ],
    excluded_artifacts = panel_exchange_client_maven_excluded_artifacts(),
    generate_compat_repositories = True,
    repositories = [
        "https://repo.maven.apache.org/maven2/",
    ],
)

load("@wfa_common_jvm//build:common_jvm_extra_deps.bzl", "common_jvm_extra_deps")

common_jvm_extra_deps()
