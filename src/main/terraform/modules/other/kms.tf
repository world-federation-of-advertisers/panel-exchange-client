resource "aws_kms_key" "k8s_key" {
  description             = "key for ocmm"
  deletion_window_in_days = 7
}

resource "aws_kms_alias" "k8s_key_alias" {
  name          = "alias/${var.kms_alias_name}"
  target_key_id = aws_kms_key.k8s_key.key_id
}
