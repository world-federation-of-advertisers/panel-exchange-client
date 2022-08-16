resource "aws_ecr_repository" "edp_image" {
  name = "edp_image"

  force_delete = true
}

resource "null_resource" "build_and_push_image" {
  depends_on = [aws_ecr_repository.edp_image]

  provisioner "local-exec" {
     command = "aws ecr get-login-password --region ${data.aws_region.current.name} | docker login --username AWS --password-stdin ${data.aws_caller_identity.current.account_id}.dkr.ecr.${data.aws_region.current.name}.amazonaws.com"
  }

  provisioner "local-exec" {
    working_dir = "../../../"
    command = "bazel run src/main/docker/${var.image_name} -c opt --define container_registry=${data.aws_caller_identity.current.account_id}.dkr.ecr.${data.aws_region.current.name}.amazonaws.com"
  }
}
