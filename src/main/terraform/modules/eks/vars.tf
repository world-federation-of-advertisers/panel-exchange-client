data "aws_availability_zones" "available" {
  state = "available"
}

variable "availability_zones_count" {
  description = "The number of AZs."
  type = number
  default = 2
}

variable "project" {
  description = "Name to be used on all the resources asan identifier."
  type = string
  default = "tftest"
}

variable "vpc_cidr" {
  description = "The CIDR block for the VPC. Default is valid, but should be overridden."
  type = string
  default = "10.0.0.0/16"
}

variable "subnet_cidr_bits" {
  description = "The number of subnet bits for the CIDR."
  type = number
  default = 8
}

variable "path_to_cmm" {
  type = string
  default = "../../../../cross-media-measurement"
}

variable "path_to_secrets" {
  type = string
  default = "~/TFTest/secrets"
}
