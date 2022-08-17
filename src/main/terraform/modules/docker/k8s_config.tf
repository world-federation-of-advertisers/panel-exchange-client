resource "null_resource" "collect_k8s_test_secrets" {
  count = var.use_test_secrets ? 1 : 0
  provisioner "local-exec" {
    working_dir = "../../../"
    command = "bazel run //src/main/k8s/testing/secretfiles:apply_kustomization"
  }
}

resource "null_resource" "collect_k8s_secrets" {
  count = var.use_test_secrets ? 0 : 1
  provisioner "local-exec" {
    command = <<EOF
cp -r ../k8s/testing/secretfiles ${var.path_to_secrets}

cat ${var.path_to_secrets}/*_root.pem > ${var.path_to_secrets}/all_root_certs.pem
    EOF
  }

  provisioner "local-exec" {
    command = <<EOF
echo "secretGenerator:" > ${var.path_to_secrets}/kustomization.yaml
echo "- name: certs-and-configs" >> ${var.path_to_secrets}/kustomization.yaml
echo "  Files:" >> ${var.path_to_secrets}/kustomization.yaml
echo "  - trusted_certs.pem" >> ${var.path_to_secrets}/kustomization.yaml
echo "  - edp1_tls.pem" >> ${var.path_to_secrets}/kustomization.yaml
echo "  - edp1_tls.key" >> ${var.path_to_secrets}/kustomization.yaml

aws eks update-kubeconfig --region ${data.aws_region.current.name} --name tftest-cluster

kubectl apply -k ${var.path_to_secrets}
    EOF
  }
}


resource "null_resource" "configure_cluster" {
  depends_on = [
    aws_ecr_repository.edp_image,
    null_resource.collect_k8s_test_secrets,
    null_resource.collect_k8s_secrets
  ]

  # login to Docker on AWS
  provisioner "local-exec" {
     command = "aws ecr get-login-password --region ${data.aws_region.current.name} | docker login --username AWS --password-stdin ${data.aws_caller_identity.current.account_id}.dkr.>"
  }

  # build and push the Docker image to ECR
  provisioner "local-exec" {
    working_dir = "../../../"
    command = "bazel run src/main/docker/${var.image_name} -c opt --define container_registry=${data.aws_caller_identity.current.account_id}.dkr.ecr.${data.aws_region.current.name}.a>"
  }

  # create a k8s service account
  provisioner "local-exec" {
    command = "kubectl create serviceaccount ${var.k8s_account_service_name}"
  }

  # build and apply secrets
  provisioner "local-exec" {
    command = <<EOF
str=$(kubectl apply -k ${var.path_to_secrets})
regex="(certs-and-configs-\S*)"
[[ $str =~ $regex ]]
secret_name=$${BASH_REMATCH[0]}

bazel build //src/main/k8s/dev:${var.build_target_name} --define=edp_name=dataProviders/c-8OD6eW4x8 --define=edp_k8s_secret_name=$secret_name

kubectl apply -f ../../../bazel-bin/src/main/k8s/dev/${var.manifest_name}
    EOF
    interpreter = ["bash", "-c"]
  }
}
