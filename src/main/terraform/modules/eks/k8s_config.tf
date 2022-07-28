resource "null_resource" "collect_k8s_secrets" {
  provisioner "local-exec" {
    command = <<EOF
cp -r ${var.path_to_cmm}/src/main/k8s/testing/secretfiles ${var.path_to_secrets}

cat ${var.path_to_secrets}/*_root.pem > ${var.path_to_secrets}/all_root_certs.pem
    EOF
  }
}

resource "null_resource" "create_k8s_secrets2" {
  provisioner "local-exec" {
    command = <<EOF
echo "secretGenerator:" > ${var.path_to_secrets}/kustomization.yaml
echo "- name: certs-and-configs" >> ${var.path_to_secrets}/kustomization.yaml
echo "  Files:" >> ${var.path_to_secrets}/kustomization.yaml
#echo "  - trusted_certs.pem" >> ${var.path_to_secrets}/kustomization.yaml
echo "  - edp1_tls.pem" >> ${var.path_to_secrets}/kustomization.yaml
echo "  - edp1_tls.key" >> ${var.path_to_secrets}/kustomization.yaml

aws eks update-kubeconfig --region us-west-1 --name tftest-cluster

kubectl apply -k ${var.path_to_secrets}
    EOF
  }
}
