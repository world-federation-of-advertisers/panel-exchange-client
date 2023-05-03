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

# TODO(@MarcoPremier): Remove grpc_health_probe dependencies in favor of the 'healthServer' added in this commit: https://github.com/world-federation-of-advertisers/common-jvm/commit/2929e0aafdd82d4317c193ac2632729a4a1e3538#diff-6b1a2b97ef5b48abd2074dc2030c6fe833ced76a800ef9b051002da548370592
load("//build:repo.bzl", "grpc_health_probe")

grpc_health_probe()

load(
    "@wfa_common_jvm//build:versions.bzl",
    "GRPC_JAVA",
    "GRPC_KOTLIN",
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
        "io.grpc:grpc-kotlin-stub:" + GRPC_KOTLIN.version,
        "io.grpc:grpc-netty:" + GRPC_JAVA.version,
        "io.grpc:grpc-services:" + GRPC_JAVA.version,
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
