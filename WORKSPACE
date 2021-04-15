workspace(name = "panel_exchange_client")

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
