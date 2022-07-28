resource "aws_ecr_repository" "edp_image" {
  name = "edp_image"

  force_delete = true
}

resource "null_resource" "build_and_push_image" {
  depends_on = [aws_ecr_repository.edp_image]

  provisioner "local-exec" {
     command = "aws ecr get-login-password --region us-west-1 | docker login --username AWS --password-stdin 010295286036.dkr.ecr.us-west-1.amazonaws.com"
  }

  provisioner "local-exec" {
    working_dir = "../../../"
    command = "tools/bazel-container run src/main/docker/push_google_cloud_example_daemon_image -c opt"
  }
}
