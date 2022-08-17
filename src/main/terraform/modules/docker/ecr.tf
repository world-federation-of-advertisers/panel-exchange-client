resource "aws_ecr_repository" "edp_image" {
  name = var.repository_name

  force_delete = true
}
