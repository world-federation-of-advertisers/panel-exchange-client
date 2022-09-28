resource "null_resource" "configure_K8S_container_insights" {
  count = var.create_metrics ? 1 : 0
  depends_on = [
    null_resource.configure_cluster
  ]

  provisioner "local-exec" {
    command = "kubectl create namespace amazon-cloudwatch"
  }  

  # https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Container-Insights-setup-metrics.html
  provisioner "local-exec" {
    command = <<EOF
# Create service account for CloudWatch agent
kubectl apply -f https://raw.githubusercontent.com/aws-samples/amazon-cloudwatch-container-insights/latest/k8s-deployment-manifest-templates/deployment-mode/daemonset/container-insights-monitoring/cwagent/cwagent-serviceaccount.yaml

# Create and apply ConfigMap for CloudWatch agent
curl -O https://raw.githubusercontent.com/aws-samples/amazon-cloudwatch-container-insights/latest/k8s-deployment-manifest-templates/deployment-mode/daemonset/container-insights-monitoring/cwagent/cwagent-configmap.yaml | \
  sed "s/{{cluster_name}}/${var.cluster_name}/" | \
  kubectl apply -f -

# Deploy CloudWatch agent as DaemonSet
kubectl apply -f https://raw.githubusercontent.com/aws-samples/amazon-cloudwatch-container-insights/latest/k8s-deployment-manifest-templates/deployment-mode/daemonset/container-insights-monitoring/cwagent/cwagent-daemonset.yaml
    EOF
  }

  # https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/ContainerInsights-Prometheus-Setup.html
  provisioner "local-exec" {
    command = <<EOF
# Create Prometheus service account
eksctl create iamserviceaccount \
--name cwagent-prometheus \
--namespace amazon-cloudwatch \
--cluster ${var.cluster_name} \
--attach-policy-arn arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy \
--approve \
--override-existing-serviceaccounts

# Create and apply ConfigMap for Prometheus agent
curl https://raw.githubusercontent.com/aws-samples/amazon-cloudwatch-container-insights/latest/k8s-deployment-manifest-templates/deployment-mode/service/cwagent-prometheus/prometheus-k8s.yaml | \
sed "s/{{cluster_name}}/${var.cluster_name}/;s/{{region_name}}/${data.aws_region.current.name}/" | \
kubectl apply -f -
    EOF
  }

  # https://aws.amazon.com/premiumsupport/knowledge-center/cloudwatch-stream-container-logs-eks/
  provisioner "local-exec" {
    command = <<EOF
kubectl apply -f https://raw.githubusercontent.com/aws-samples/amazon-cloudwatch-container-insights/latest/k8s-deployment-manifest-templates/deployment-mode/daemonset/container-insights-monitoring/cloudwatch-namespace.yaml

ClusterName=tftest2-cluster
RegionName=us-west-2
FluentBitHttpPort='2020'
FluentBitReadFromHead='Off'
[[ $${FluentBitReadFromHead} = 'On' ]] && FluentBitReadFromTail='Off'|| FluentBitReadFromTail='On'
[[ -z $${FluentBitHttpPort} ]] && FluentBitHttpServer='Off' || FluentBitHttpServer='On'
kubectl create configmap fluent-bit-cluster-info \
--from-literal=cluster.name=$${ClusterName} \
--from-literal=http.server=$${FluentBitHttpServer} \
--from-literal=http.port=$${FluentBitHttpPort} \
--from-literal=read.head=$${FluentBitReadFromHead} \
--from-literal=read.tail=$${FluentBitReadFromTail} \
--from-literal=logs.region=$${RegionName} -n amazon-cloudwatch

kubectl apply -f https://raw.githubusercontent.com/aws-samples/amazon-cloudwatch-container-insights/latest/k8s-deployment-manifest-templates/deployment-mode/daemonset/container-insights-monitoring/fluent-bit/fluent-bit.yaml

kubectl patch ds fluent-bit -n amazon-cloudwatch -p \
'{"spec":{"template":{"spec":{"containers":[{"name":"fluent-bit","image":"public.ecr.aws/aws-observability/aws-for-fluent-bit:latest"}]}}}}'

regex="(.*)-cluster"
[[ ${var.cluster_name} =~ $regex ]]
project_name=$${BASH_REMATCH[0]}
kubectl annotate serviceaccounts fluent-bit -n amazon-cloudwatch "eks.amazonaws.com/role-arn=arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/$${project_name}-Worker-Role"
    EOF
  }
}
