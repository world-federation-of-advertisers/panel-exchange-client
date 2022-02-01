# GKE Deployment

## Deploy Panel Match

This is a job that tests correctness by creating a Measurement and validating
the result.

```shell
bazel run //src/main/k8s/local:mc_frontend_simulator_kind \
  --define=k8s_secret_name=certs-and-configs-k8888kc6gg \
  --define=mc_name=measurementConsumers/FS1n8aTrck0
```
