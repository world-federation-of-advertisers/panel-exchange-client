resource "aws_kms_key" "my_key" {
  description             = "key for ocmm"
  deletion_window_in_days = 7
}
