terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 5.0" }
  }
  backend "s3" {
    bucket  = "vatika-work-order-tfstate"
    key     = "staging/persistent/terraform.tfstate"
    region  = "ca-central-1"
    encrypt = true
  }
}