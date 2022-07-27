terraform {
  backend "s3" {
    bucket = "tf-ocmm-test-bucket"
    key = "panel-exchange.tfstate"
    region = "us-west-1"
    encrypt = true
  }
}

provider "aws" {
  region = "us-west-1"
}
