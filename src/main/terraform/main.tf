module "aws_eks_cluster" {
  source = "./modules/eks"
}

module "docker_config" {
  depends_on = [module.aws_eks_cluster]
  source = "./modules/docker"

  image_name = "push_google_cloud_example_daemon_image"
  build_target_name = "example_edp_daemon_gke"
  manifest_name = "example_edp_daemon_aws.yaml"
}

module "other_resources" {
  depends_on = [module.docker_config]
  source = "./modules/other"

  bucket_name = "tf-test-blob-storage"
  kms_alias_name = "my-key-alias"
}
