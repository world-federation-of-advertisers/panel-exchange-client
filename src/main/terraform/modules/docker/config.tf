resource "null_resource" "create_k8s_secrets2" {
  # run out of the box test secret files
  # provisioner "local-exec" {
  #   working_dir = "../../../"
  #   command = "tools/bazel-container run //src/main/k8s/testing/secretfiles:apply_kustomization"
  # }

  # apply secrets
  # buildl the deployment and services manifest
  provisioner "local-exec" {
    command = <<EOF
str=$(kubectl apply -k ../../../../TFTest/secrets)
regex="(certs-and-configs-\S*)"
[[ $str =~ $regex ]]
secret_name=$${BASH_REMATCH[0]}

bazel build //src/main/k8s/dev:example_edp_daemon_gke --define=edp_name=dataProviders/c-8OD6eW4x8 --define=edp_k8s_secret_name=$secret_name

kubectl apply -f ../../../bazel-bin/src/main/k8s/dev/example_edp_daemon_aws.yaml
    EOF
    interpreter = ["bash", "-c"]
  }
}

