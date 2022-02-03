# GKE Deployment

## Deploy Panel Match

This is a daemon job that keeps pulling available jobs from Measurement 
Coordinator and processes them.

```shell
bazel run src/main/docker/push_google_cloud_example_daemon_image \ 
  --define=container_registry=gcr.io \
  --define=image_repo_prefix=ads-open-measurement
```

```shell
bazel run src/main/kotlin/org/wfanet/panelmatch/tools:deploy_panelmatch_dev_to_gke \
  --define=container_registry=gcr.io \
  --define=image_repo_prefix=ads-open-measurement
```
