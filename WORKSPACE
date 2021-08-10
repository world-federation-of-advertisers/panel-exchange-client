workspace(name = "panel_exchange_client")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# @bazel_skylib

http_archive(
    name = "bazel_skylib",
    sha256 = "1c531376ac7e5a180e0237938a2536de0c54d93f5c278634818e0efc952dd56c",
    urls = [
        "https://github.com/bazelbuild/bazel-skylib/releases/download/1.0.3/bazel-skylib-1.0.3.tar.gz",
        "https://mirror.bazel.build/github.com/bazelbuild/bazel-skylib/releases/download/1.0.3/bazel-skylib-1.0.3.tar.gz",
    ],
)

load("@bazel_skylib//:workspace.bzl", "bazel_skylib_workspace")

bazel_skylib_workspace()

# @platforms

http_archive(
    name = "platforms",
    sha256 = "079945598e4b6cc075846f7fd6a9d0857c33a7afc0de868c2ccb96405225135d",
    urls = [
        "https://mirror.bazel.build/github.com/bazelbuild/platforms/releases/download/0.0.4/platforms-0.0.4.tar.gz",
        "https://github.com/bazelbuild/platforms/releases/download/0.0.4/platforms-0.0.4.tar.gz",
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

# Common JVM
http_archive(
    name = "wfa_common_jvm",
    sha256 = "b4f410343536bb11bb0a8a868e611be792e1cb0d493d329b9ad2fe4c4dbb7c35",
    strip_prefix = "common-jvm-a2b9bae790fc84205499bed09bd1ac22e9cf7328",
    url = "https://github.com/world-federation-of-advertisers/common-jvm/archive/a2b9bae790fc84205499bed09bd1ac22e9cf7328.tar.gz",
)

# Measurement system.
http_archive(
    name = "wfa_measurement_system",
    sha256 = "6e76b7ad3d7f4b005a091343ec8845fac0d1d6c5f24c6ac25d1f27b41a72585c",
    strip_prefix = "cross-media-measurement-3fd2a7fde637f633588a64e39620f47bddaeb004",
    url = "https://github.com/world-federation-of-advertisers/cross-media-measurement/archive/3fd2a7fde637f633588a64e39620f47bddaeb004.tar.gz",
)

# Measurement proto.
http_archive(
    name = "wfa_measurement_proto",
    sha256 = "94b6ed87c4c9917da80fc4f5803b2c62a93767f433bfd7f25e5c6c9dc355aa38",
    strip_prefix = "cross-media-measurement-api-640987b5196e26fe717a47875f603360d6c11346",
    url = "https://github.com/world-federation-of-advertisers/cross-media-measurement-api/archive/640987b5196e26fe717a47875f603360d6c11346.tar.gz",
)

# @io_bazel_rules_kotlin

load("@wfa_common_jvm//build/io_bazel_rules_kotlin:repo.bzl", "rules_kotlin_repo")

rules_kotlin_repo()

load("@wfa_common_jvm//build/io_bazel_rules_kotlin:deps.bzl", "rules_kotlin_deps")

rules_kotlin_deps()

# kotlinx.coroutines
load("@wfa_common_jvm//build/kotlinx_coroutines:repo.bzl", "kotlinx_coroutines_artifact_dict")

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
    sha256 = "f36441aa876c4f6427bfb2d1f2d723b48e9d930b62662bf723ddfb8fc80f0140",
    strip_prefix = "rules_jvm_external-4.1",
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/4.1.zip",
)

load("@rules_jvm_external//:defs.bzl", "maven_install")
load("@wfa_common_jvm//build/maven:artifacts.bzl", "artifacts")

MAVEN_ARTIFACTS = artifacts.list_to_dict(
    IO_GRPC_GRPC_JAVA_ARTIFACTS +
    IO_GRPC_GRPC_KOTLIN_ARTIFACTS,
)

MAVEN_ARTIFACTS.update(com_google_truth_artifact_dict(version = "1.0.1"))

MAVEN_ARTIFACTS.update(kotlinx_coroutines_artifact_dict(version = "1.4.3"))

BEAM_VERSION = "2.31.0"

# Add Maven artifacts or override versions (e.g. those pulled in by gRPC Kotlin
# or default dependency versions).
MAVEN_ARTIFACTS.update({
    # Without this, we get java.lang.NoClassDefFoundError: com/google/api/gax/tracing/NoopApiTracer
    "com.google.api:gax": "2.0.0",
    "com.google.api:gax-grpc": "2.0.0",
    "com.google.apis:google-api-services-bigquery": "v2-rev20210404-1.31.0",
    "com.google.cloud:google-cloud-bigquery": "1.137.1",
    "com.google.cloud:google-cloud-nio": "0.122.0",
    "com.google.cloud:google-cloud-storage": "1.118.0",
    "com.google.code.gson:gson": "2.8.6",
    "com.google.guava:guava": "30.1.1-jre",
    "com.google.http-client:google-http-client": "1.39.2",
    "info.picocli:picocli": "4.4.0",
    "io.grpc:grpc-api": "1.37.0",
    "joda-time:joda-time": "2.10.10",
    "junit:junit": "4.13.1",
    "org.apache.beam:beam-runners-direct-java": BEAM_VERSION,
    "org.apache.beam:beam-runners-google-cloud-dataflow-java": BEAM_VERSION,
    "org.apache.beam:beam-sdks-java-core": BEAM_VERSION,
    "org.apache.beam:beam-sdks-java-extensions-google-cloud-platform-core": BEAM_VERSION,
    "org.apache.beam:beam-sdks-java-extensions-protobuf": BEAM_VERSION,
    "org.apache.beam:beam-sdks-java-io-google-cloud-platform": BEAM_VERSION,
    "org.apache.beam:beam-vendor-guava-26_0-jre": "0.1",
    "org.hamcrest:hamcrest": "2.2",
    "org.mockito.kotlin:mockito-kotlin": "3.2.0",
    "org.slf4j:slf4j-simple": "1.7.32",

    # For grpc-kotlin. This should be a version that is compatible with the
    # Kotlin release used by rules_kotlin.
    "com.squareup:kotlinpoet": "1.8.0",
})

maven_install(
    artifacts = artifacts.dict_to_list(MAVEN_ARTIFACTS),
    excluded_artifacts = [
        "org.apache.beam:beam-sdks-java-io-kafka",
    ],
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

# @io_bazel_rules_docker

load("//build/io_bazel_rules_docker:repo.bzl", "rules_docker_repo")

rules_docker_repo(
    name = "io_bazel_rules_docker",
    commit = "f929d80c5a4363994968248d87a892b1c2ef61d4",
    sha256 = "efda18e39a63ee3c1b187b1349f61c48c31322bf84227d319b5dece994380bb6",
)

load(
    "@io_bazel_rules_docker//repositories:repositories.bzl",
    container_repositories = "repositories",
)

container_repositories()

load("@io_bazel_rules_docker//repositories:deps.bzl", container_deps = "deps")

container_deps()

load("//build/io_bazel_rules_docker:base_images.bzl", "base_java_images")

# Defualt base images for java_image targets. Must come before
# java_image_repositories().
base_java_images(
    # gcr.io/distroless/java:11-debug
    debug_digest = "sha256:c3fe781de55d375de2675c3f23beb3e76f007e53fed9366ba931cc6d1df4b457",
    # gcr.io/distroless/java:11
    digest = "sha256:7fc091e8686df11f7bf0b7f67fd7da9862b2b9a3e49978d1184f0ff62cb673cc",
)

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

# @com_google_truth_truth
load("@wfa_common_jvm//build/com_google_truth:repo.bzl", "com_google_truth_artifact_dict")

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

load("@wfa_common_jvm//build/wfa:repositories.bzl", "wfa_repo_archive")

wfa_repo_archive(
    name = "wfa_rules_swig",
    commit = "653d1bdcec85a9373df69920f35961150cf4b1b6",
    repo = "rules_swig",
    sha256 = "34c15134d7293fc38df6ed254b55ee912c7479c396178b7f6499b7e5351aeeec",
)

# Tink
http_archive(
    name = "tink_base",
    sha256 = "005e6c49b2b2df8a7dc670471ee45b6e09092bb05046eea358cd47f2703359c4",
    strip_prefix = "tink-7c93a224b8fa6a3babfaf71c18c5610052dcbd61/",
    urls = ["https://github.com/google/tink/archive/7c93a224b8fa6a3babfaf71c18c5610052dcbd61.zip"],
)

http_archive(
    name = "tink_cc",
    sha256 = "005e6c49b2b2df8a7dc670471ee45b6e09092bb05046eea358cd47f2703359c4",
    strip_prefix = "tink-7c93a224b8fa6a3babfaf71c18c5610052dcbd61/cc",
    urls = ["https://github.com/google/tink/archive/7c93a224b8fa6a3babfaf71c18c5610052dcbd61.zip"],
)

load("@tink_base//:tink_base_deps.bzl", "tink_base_deps")

tink_base_deps()

load("@tink_base//:tink_base_deps_init.bzl", "tink_base_deps_init")

tink_base_deps_init()

load("@tink_cc//:tink_cc_deps.bzl", "tink_cc_deps")

tink_cc_deps()

load("@tink_cc//:tink_cc_deps_init.bzl", "tink_cc_deps_init")

tink_cc_deps_init()

# Common-cpp
http_archive(
    name = "wfa_common_cpp",
    sha256 = "e0e1f5eed832ef396109354a64c6c1306bf0fb5ea0b449ce6ee1e8edc6fe279d",
    strip_prefix = "common-cpp-43c75acc3394e19bcfd2cfe8e8e2454365d26d60",
    url = "https://github.com/world-federation-of-advertisers/common-cpp/archive/43c75acc3394e19bcfd2cfe8e8e2454365d26d60.tar.gz",
)

load("@wfa_common_cpp//build:deps.bzl", "common_cpp_deps")

common_cpp_deps()
