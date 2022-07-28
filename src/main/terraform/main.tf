module "aws_eks_cluster" {
  source = "./modules/eks"
}

module "docker_config" {
  depends_on = [module.aws_eks_cluster]
  source = "./modules/docker"
}
