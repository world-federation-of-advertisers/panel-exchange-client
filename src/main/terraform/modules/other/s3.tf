resource "aws_s3_bucket" "blob_storage" {
  bucket = var.bucket_name
}
