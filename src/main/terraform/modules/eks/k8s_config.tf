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
