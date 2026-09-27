provider "aws" {
  region = var.region
}

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

module "ecs" {
  source            = "../../modules/ecs-service"
  name              = var.name
  region            = var.region
  image             = "${module.ecr.repository_url}:${var.image_tag}"
  subnet_ids        = module.network.public_subnet_ids
  security_group_id = module.network.app_sg_id
  db_endpoint       = module.rds.endpoint
  db_name           = "workorder"
  db_username       = "workorder"
  db_password       = var.db_password
  bucket_name       = module.s3.bucket_name
  bucket_arn        = module.s3.bucket_arn
}

module "github_oidc" {
  source             = "../../modules/github-oidc"
  name               = var.name
  github_repo        = "Vatika1/work-order-platform"
  ecr_repository_arn = module.ecr.repository_arn
  ecs_service_arn    = module.ecs.service_arn
}