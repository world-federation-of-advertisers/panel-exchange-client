data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

variable "use_test_secrets" {
  description = "Whether or not to use the test secrets. They should not be used outside of testing purposes."
  type = bool
  default = false
}

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

variable "path_to_secrets" {
  type = string
}

variable "k8s_account_service_name" {
  type = string
}

variable "cluster_name" {
  type = string
}

variable "kms_key_id" {
  type = string
}

variable "path_to_edp_cue" {
  type = string
  default = "../k8s/dev/example_edp_daemon_aws.cue"
}
