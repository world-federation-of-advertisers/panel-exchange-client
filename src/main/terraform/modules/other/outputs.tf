output "kms_key_id" {
  value = aws_kms_key.k8s_key.key_id
}
