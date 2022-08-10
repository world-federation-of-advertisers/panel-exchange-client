module "aws_eks_cluster" {
  source = "./modules/eks"
}

module "docker_config" {
  depends_on = [module.aws_eks_cluster]
  source = "./modules/docker"
}

module "other_resources" {
  depends_on = [module.docker_config]
  source = "./modules/other"
}
