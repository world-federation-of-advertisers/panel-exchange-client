workspace(name = "panel_exchange_client")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive", "http_file")

# @bazel_skylib

http_archive(
    name = "bazel_skylib",
    sha256 = "1c531376ac7e5a180e0237938a2536de0c54d93f5c278634818e0efc952dd56c",
    urls = [
        "https://github.com/bazelbuild/bazel-skylib/releases/download/1.0.3/bazel-skylib-1.0.3.tar.gz",
        "https://mirror.bazel.build/github.com/bazelbuild/bazel-skylib/releases/download/1.0.3/bazel-skylib-1.0.3.tar.gz",
    ],
)

http_archive(
    name = "com_google_protobuf",
    sha256 = "65e020a42bdab44a66664d34421995829e9e79c60e5adaa08282fd14ca552f57",
    strip_prefix = "protobuf-3.15.6",
    urls = [
        "https://github.com/protocolbuffers/protobuf/archive/refs/tags/v3.15.6.tar.gz",
    ],
)

http_archive(
    name = "googletest",
    sha256 = "94c634d499558a76fa649edb13721dce6e98fb1e7018dfaeba3cd7a083945e91",
    strip_prefix = "googletest-release-1.10.0",
    urls = ["https://github.com/google/googletest/archive/release-1.10.0.zip"],
)

# Abseil C++ libraries
http_archive(
    name = "com_google_absl",
    sha256 = "dd7db6815204c2a62a2160e32c55e97113b0a0178b2f090d6bab5ce36111db4b",
    strip_prefix = "abseil-cpp-20210324.0",
    urls = [
        "https://github.com/abseil/abseil-cpp/archive/refs/tags/20210324.0.tar.gz",
    ],
)

# @com_google_truth_truth
load("//build/com_google_truth:repo.bzl", "com_google_truth_artifact_dict")

# Measurement system.
http_archive(
    name = "wfa_measurement_system",
    sha256 = "d0200afef07d5a2c81adbe6c0c319a663e058195ad563401bfc4813bc4de6cb9",
    strip_prefix = "cross-media-measurement-933284c02cff0be89991c31178bb9538de70f01b",
    url = "https://github.com/world-federation-of-advertisers/cross-media-measurement/archive/933284c02cff0be89991c31178bb9538de70f01b.tar.gz",
)

# @io_bazel_rules_kotlin

load("@wfa_measurement_system//build/io_bazel_rules_kotlin:repo.bzl", "kotlinc_release", "rules_kotlin_repo")

rules_kotlin_repo(
    sha256 = "9cc0e4031bcb7e8508fd9569a81e7042bbf380604a0157f796d06d511cff2769",
    version = "legacy-1.4.0-rc4",
)

load("@wfa_measurement_system//build/io_bazel_rules_kotlin:deps.bzl", "rules_kotlin_deps")

rules_kotlin_deps(compiler_release = kotlinc_release(
    sha256 = "ccd0db87981f1c0e3f209a1a4acb6778f14e63fe3e561a98948b5317e526cc6c",
    version = "1.3.72",
))

# kotlinx.coroutines
load("@wfa_measurement_system//build/kotlinx_coroutines:repo.bzl", "kotlinx_coroutines_artifact_dict")

# @com_github_grpc_grpc_kotlin

http_archive(
    name = "com_github_grpc_grpc_kotlin",
    sha256 = "08f06a797ec806d68e8811018cefd1d5a6b8bf1782b63937f2618a6be86a9e2d",
    strip_prefix = "grpc-kotlin-0.2.1",
    url = "https://github.com/grpc/grpc-kotlin/archive/v0.2.1.zip",
)

load(
    "@com_github_grpc_grpc_kotlin//:repositories.bzl",
    "IO_GRPC_GRPC_KOTLIN_ARTIFACTS",
    "IO_GRPC_GRPC_KOTLIN_OVERRIDE_TARGETS",
    "grpc_kt_repositories",
    "io_grpc_grpc_java",
)

io_grpc_grpc_java()

load(
    "@io_grpc_grpc_java//:repositories.bzl",
    "IO_GRPC_GRPC_JAVA_ARTIFACTS",
    "IO_GRPC_GRPC_JAVA_OVERRIDE_TARGETS",
    "grpc_java_repositories",
)

# Maven

http_archive(
    name = "rules_jvm_external",
    sha256 = "82262ff4223c5fda6fb7ff8bd63db8131b51b413d26eb49e3131037e79e324af",
    strip_prefix = "rules_jvm_external-3.2",
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/3.2.zip",
)

load("@rules_jvm_external//:defs.bzl", "maven_install")
load("@wfa_measurement_system//build/maven:artifacts.bzl", "artifacts")

MAVEN_ARTIFACTS = artifacts.list_to_dict(
    IO_GRPC_GRPC_JAVA_ARTIFACTS +
    IO_GRPC_GRPC_KOTLIN_ARTIFACTS,
)

MAVEN_ARTIFACTS.update(com_google_truth_artifact_dict(version = "1.0.1"))

MAVEN_ARTIFACTS.update(kotlinx_coroutines_artifact_dict(version = "1.3.6"))

# Add Maven artifacts or override versions (e.g. those pulled in by gRPC Kotlin
# or default dependency versions).
MAVEN_ARTIFACTS.update({
    "com.google.api.grpc:grpc-google-cloud-pubsub-v1": "0.1.24",
    "com.google.cloud:google-cloud-nio": "0.122.0",
    "com.google.cloud:google-cloud-spanner": "3.0.3",
    "com.google.code.gson:gson": "2.8.6",
    "com.google.guava:guava": "30.0-jre",
    "com.nhaarman.mockitokotlin2:mockito-kotlin": "2.2.0",
    "info.picocli:picocli": "4.4.0",
    "junit:junit": "4.13",
    "org.apache.beam:beam-runners-direct-java": "2.29.0",
    "org.apache.beam:beam-sdks-java-core": "2.29.0",
})

maven_install(
    artifacts = artifacts.dict_to_list(MAVEN_ARTIFACTS),
    fetch_sources = True,
    generate_compat_repositories = True,
    override_targets = dict(
        IO_GRPC_GRPC_JAVA_OVERRIDE_TARGETS.items() +
        IO_GRPC_GRPC_KOTLIN_OVERRIDE_TARGETS.items(),
    ),
    repositories = [
        "https://repo.maven.apache.org/maven2/",
    ],
)

load("@maven//:compat.bzl", "compat_repositories")

compat_repositories()

# Run after compat_repositories to ensure the maven_install-selected
# dependencies are used.
grpc_kt_repositories()

grpc_java_repositories()  # For gRPC Kotlin.

# gRPC
http_archive(
    name = "com_github_grpc_grpc",
    sha256 = "8eb9d86649c4d4a7df790226df28f081b97a62bf12c5c5fe9b5d31a29cd6541a",
    strip_prefix = "grpc-1.36.4",
    urls = ["https://github.com/grpc/grpc/archive/v1.36.4.tar.gz"],
)

load("@com_github_grpc_grpc//bazel:grpc_deps.bzl", "grpc_deps")

grpc_deps()

load("@com_github_grpc_grpc//bazel:grpc_extra_deps.bzl", "grpc_extra_deps")

grpc_extra_deps()

load("//build/com_google_private_join_and_compute:repo.bzl", "private_join_and_compute_repo")

private_join_and_compute_repo(
    commit = "89c8d0aae070b9c282043af419e47d7ef897f460",
    sha256 = "13e0414220a2709b0dbeefafe5a4d1b3f3261a541d0405c844857521d5f25f32",
)

# @platforms

http_archive(
    name = "com_google_protobuf",
    strip_prefix = "protobuf-3.15.6",
    urls = [
        "https://github.com/protocolbuffers/protobuf/archive/refs/tags/v3.15.6.tar.gz",
    ],
)

# Measurement system.
http_archive(
    name = "wfa_measurement_system",
    sha256 = "d0200afef07d5a2c81adbe6c0c319a663e058195ad563401bfc4813bc4de6cb9",
    strip_prefix = "cross-media-measurement-933284c02cff0be89991c31178bb9538de70f01b",
    url = "https://github.com/world-federation-of-advertisers/cross-media-measurement/archive/933284c02cff0be89991c31178bb9538de70f01b.tar.gz",
)

# Measurement proto.
http_archive(
    name = "wfa_measurement_proto",
    sha256 = "c7d87a438a446ebeacdcae8bcfed270c513ae5c5d26bccd36fb179d47e7d3365",
    strip_prefix = "cross-media-measurement-api-ab647fffd78f29769611f05ef131ec1f1feed820",
    url = "https://github.com/world-federation-of-advertisers/cross-media-measurement-api/archive/ab647fffd78f29769611f05ef131ec1f1feed820.tar.gz",
)

# @com_google_truth_truth
load("@wfa_measurement_system//build/com_google_truth:repo.bzl", "com_google_truth_artifact_dict")

# @io_bazel_rules_kotlin

load("@wfa_measurement_system//build/io_bazel_rules_kotlin:repo.bzl", "kotlinc_release", "rules_kotlin_repo")

rules_kotlin_repo(
    sha256 = "9cc0e4031bcb7e8508fd9569a81e7042bbf380604a0157f796d06d511cff2769",
    version = "legacy-1.4.0-rc4",
)

load("@wfa_measurement_system//build/io_bazel_rules_kotlin:deps.bzl", "rules_kotlin_deps")

rules_kotlin_deps(compiler_release = kotlinc_release(
    sha256 = "ccd0db87981f1c0e3f209a1a4acb6778f14e63fe3e561a98948b5317e526cc6c",
    version = "1.3.72",
))

load("@com_google_protobuf//:protobuf_deps.bzl", "protobuf_deps")  # From gRPC.

# Google API protos
http_archive(
    name = "com_google_googleapis",
    sha256 = "65b3c3c4040ba3fc767c4b49714b839fe21dbe8467451892403ba90432bb5851",
    strip_prefix = "googleapis-a1af63efb82f54428ab35ea76869d9cd57ca52b8",
    urls = ["https://github.com/googleapis/googleapis/archive/a1af63efb82f54428ab35ea76869d9cd57ca52b8.tar.gz"],
)

# Google APIs imports. Required to build googleapis.
load("@com_google_googleapis//:repository_rules.bzl", "switched_rules_by_language")

switched_rules_by_language(
    name = "com_google_googleapis_imports",
    java = True,
)

load("@wfa_measurement_system//build/wfa:repositories.bzl", "wfa_repo_archive")

wfa_repo_archive(
    name = "wfa_rules_swig",
    commit = "653d1bdcec85a9373df69920f35961150cf4b1b6",
    repo = "rules_swig",
    sha256 = "34c15134d7293fc38df6ed254b55ee912c7479c396178b7f6499b7e5351aeeec",
)
