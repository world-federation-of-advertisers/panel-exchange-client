module "aws_eks_cluster" {
  source = "./modules/eks"

  availability_zones_count = 2
  project = "tftest"
  vpc_cidr = "10.0.0.0/16"
  subnet_cidr_bits = 8
}

module "other_resources" {
  source = "./modules/other"

  bucket_name = "tf-test-blob-storage"
  kms_alias_name = "my-key-alias"
}

module "docker_config" {
  for_each = toset(["example-panel-exchange-daemon"])
  depends_on = [
    module.aws_eks_cluster,
    module.other_resources
  ]
  source = "./modules/docker"

  use_test_secrets = true
  image_name = "push_aws_example_daemon_image"
  build_target_name = "example_edp_daemon_aws"
  manifest_name = "example_edp_daemon_aws.yaml"
  repository_name = each.key
  path_to_secrets = "../k8s/testing/secretfiles"
  k8s_account_service_name = "edp-workflow"
  cluster_name = module.aws_eks_cluster.cluster_name
  kms_key_id = module.other_resources.kms_key_id
}
