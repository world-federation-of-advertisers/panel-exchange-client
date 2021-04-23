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

# Abseil C++ libraries
http_archive(
    name = "com_google_absl",
    sha256 = "dd7db6815204c2a62a2160e32c55e97113b0a0178b2f090d6bab5ce36111db4b",
    strip_prefix = "abseil-cpp-20210324.0",
    urls = [
        "https://github.com/abseil/abseil-cpp/archive/refs/tags/20210324.0.tar.gz",
    ],
)

# gflags
# Needed for glog
http_archive(
    name = "com_github_gflags_gflags",
    sha256 = "34af2f15cf7367513b352bdcd2493ab14ce43692d2dcd9dfc499492966c64dcf",
    strip_prefix = "gflags-2.2.2",
    urls = ["https://github.com/gflags/gflags/archive/v2.2.2.tar.gz"],
)

# glog
# Needed for private-join-and-compute
http_archive(
    name = "com_github_glog_glog",
    sha256 = "f28359aeba12f30d73d9e4711ef356dc842886968112162bc73002645139c39c",
    strip_prefix = "glog-0.4.0",
    urls = ["https://github.com/google/glog/archive/v0.4.0.tar.gz"],
)

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

http_archive(
    name = "com_google_private_join_and_compute",
    sha256 = "13e0414220a2709b0dbeefafe5a4d1b3f3261a541d0405c844857521d5f25f32",
    strip_prefix = "private-join-and-compute-89c8d0aae070b9c282043af419e47d7ef897f460",
    urls = [
        "https://github.com/google/private-join-and-compute/archive/89c8d0aae070b9c282043af419e47d7ef897f460.zip",
    ],
)

# @io_bazel_rules_kotlin

load("//build/io_bazel_rules_kotlin:repo.bzl", "kotlinc_release", "rules_kotlin_repo")

rules_kotlin_repo(
    sha256 = "9cc0e4031bcb7e8508fd9569a81e7042bbf380604a0157f796d06d511cff2769",
    version = "legacy-1.4.0-rc4",
)

load("//build/io_bazel_rules_kotlin:deps.bzl", "rules_kotlin_deps")

rules_kotlin_deps(compiler_release = kotlinc_release(
    sha256 = "ccd0db87981f1c0e3f209a1a4acb6778f14e63fe3e561a98948b5317e526cc6c",
    version = "1.3.72",
))
