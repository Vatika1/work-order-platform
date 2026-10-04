provider "aws" {
  region = var.region
}

data "aws_caller_identity" "current" {}

module "network" {
  source             = "../../modules/network"
  name               = var.name
  availability_zones = ["${var.region}a", "${var.region}b"]
}

module "ecr" {
  source = "../../modules/ecr"
  name   = var.name
}

module "s3" {
  source      = "../../modules/s3"
  bucket_name = "${var.name}-files"
}

module "rds" {
  source            = "../../modules/rds"
  name              = var.name
  subnet_ids        = module.network.public_subnet_ids
  security_group_id = module.network.db_sg_id
  db_password       = var.db_password
}

# ECS lives in staging-ephemeral, so the service ARN is built from the naming
# convention (cluster = <name>-cluster, service = <name>-service) instead of module.ecs.
module "github_oidc" {
  source             = "../../modules/github-oidc"
  name               = var.name
  github_repo        = "Vatika1/work-order-platform"
  ecr_repository_arn = module.ecr.repository_arn
  ecs_service_arn    = "arn:aws:ecs:${var.region}:${data.aws_caller_identity.current.account_id}:service/${var.name}-cluster/${var.name}-service"
}