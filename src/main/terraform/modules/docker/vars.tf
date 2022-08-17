data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

variable "image_name" {
  description = "The name of the image to build, push, and deploy."
  type = string
}

variable "build_target_name" {
  description = "The name of the bazel target to run."
  type = string
}

variable "manifest_name" {
  description = "The name of the manifest to apply."
  type = string
}

variable "repository_name" {
  description = "The name of the respository you want to create."
  type = string
}
