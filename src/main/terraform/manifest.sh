#!/bin/bash

aws_region="us-west-1"
aws_account_id=""

image_name="push_aws_example_daemon_image"
rebuild_image=0

path_to_secrets=".."

build_target_name="example_edp_daemon_aws"
deployment_name="example-panel-exchange-daemon-deployment"

while getopts ":a:" opt; do
  case $opt in
    a)
      aws_account_id=$OPTARG
      ;;
    p)
      path_to_secrets=$OPTARG
      ;;
    r)
      rebuild_image=1
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      exit 1
      ;;
  esac
done

if [ -z "$aws_account_id" ]; then
  echo 'Missing -a' >&2
  exit 1
fi

if [[ $rebuild_image -eq 1 ]]
then
  # Log into Docker
  aws ecr get-login-password --region $aws_region | docker login --username AWS --password-stdin $aws_account_id.dkr.ecr.$aws_region.amazonaws.com

  # Build and push the image
  bazel run src/main/docker/$image_name -c opt --define container_registry=$aws_account_id.dkr.ecr.$aws_region.amazonaws.com
fi

# Build and apply secrets
str=$(kubectl apply -k $path_to_secrets)
regex="(certs-and-configs-\S*)"
[[ $str =~ $regex ]]
secret_name=$${BASH_REMATCH[0]}
bazel build //src/main/k8s/dev:$build_target_name --define=edp_name=dataProviders/c-8OD6eW4x8 --define=edp_k8s_secret_name=$secret_name
kubectl apply -f ../../../bazel-bin/src/main/k8s/dev/$build_target_name.yaml

# Redeploy Kubernetes
kubectl rollout restart deployment $deployment_name
