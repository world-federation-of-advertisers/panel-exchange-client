variable "bucket_name" {
  description = "The name of the S3 bucket."
  type = string
}

variable "kms_alias_name" {
  description = "The name of the alias for the KMS key."
  type = string
}
