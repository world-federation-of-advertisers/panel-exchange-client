load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

def panel_exchange_client_repositories():
    """Imports all direct dependencies for panel_exchange_client."""
    http_archive(
        name = "wfa_common_cpp",
        sha256 = "3110be93990a449ac8f60b534319d7d3a08aa118908fecd7b571a5e08260e560",
        strip_prefix = "common-cpp-0.2.1",
        url = "https://github.com/world-federation-of-advertisers/common-cpp/archive/v0.2.1.tar.gz",
    )

    http_archive(
        name = "wfa_common_jvm",
        sha256 = "d08bbabe8f78592fe58109ddad17dab1a4a2d0e910d602bf3fa3b3f00ff5ddf3",
        strip_prefix = "common-jvm-0.6.0",
        url = "https://github.com/world-federation-of-advertisers/common-jvm/archive/v0.6.0.tar.gz",
    )

    # TODO: remove dependencies on wfa_measurement_system
    http_archive(
        name = "wfa_measurement_system",
        sha256 = "6e76b7ad3d7f4b005a091343ec8845fac0d1d6c5f24c6ac25d1f27b41a72585c",
        strip_prefix = "cross-media-measurement-3fd2a7fde637f633588a64e39620f47bddaeb004",
        url = "https://github.com/world-federation-of-advertisers/cross-media-measurement/archive/3fd2a7fde637f633588a64e39620f47bddaeb004.tar.gz",
    )

    http_archive(
        name = "wfa_measurement_proto",
        sha256 = "94b6ed87c4c9917da80fc4f5803b2c62a93767f433bfd7f25e5c6c9dc355aa38",
        strip_prefix = "cross-media-measurement-api-640987b5196e26fe717a47875f603360d6c11346",
        url = "https://github.com/world-federation-of-advertisers/cross-media-measurement-api/archive/640987b5196e26fe717a47875f603360d6c11346.tar.gz",
    )

    http_archive(
        name = "wfa_rules_swig",
        sha256 = "34c15134d7293fc38df6ed254b55ee912c7479c396178b7f6499b7e5351aeeec",
        strip_prefix = "rules_swig-653d1bdcec85a9373df69920f35961150cf4b1b6",
        url = "https://github.com/world-federation-of-advertisers/rules_swig/archive/653d1bdcec85a9373df69920f35961150cf4b1b6.tar.gz",
    )

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
