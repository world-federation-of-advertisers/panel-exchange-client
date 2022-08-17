module "aws_eks_cluster" {
  source = "./modules/eks"

  availability_zones_count = 2
  project = "tftest"
  vpc_cidr = "10.0.0.0/16"
  subnet_cidr_bits = 8
  path_to_secrets = "~/TFTest/secrets"
}

module "docker_config" {
  depends_on = [module.aws_eks_cluster]
  source = "./modules/docker"

  image_name = "push_aws_example_daemon_image"
  build_target_name = "example_edp_daemon_aws"
  manifest_name = "example_edp_daemon_aws.yaml"
  repository_name = "tf-test-repo"
}

module "other_resources" {
  depends_on = [module.docker_config]
  source = "./modules/other"

  bucket_name = "tf-test-blob-storage"
  kms_alias_name = "my-key-alias"
}
