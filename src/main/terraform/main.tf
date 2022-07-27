module "aws_eks_cluster" {
  source = "./modules/eks"
}

module "docker_config" {
  source = "./modules/docker"
}
