provider "aws" {
  region = var.region
}

locals {
  persistent = data.terraform_remote_state.persistent.outputs
}

module "ecs" {
  source            = "../../modules/ecs-service"
  name              = var.name
  region            = var.region
  vpc_id            = local.persistent.vpc_id
  image             = "${local.persistent.ecr_repository_url}:${var.image_tag}"
  subnet_ids        = local.persistent.public_subnet_ids
  security_group_id = local.persistent.app_sg_id
  db_endpoint       = local.persistent.db_endpoint
  db_name           = "workorder"
  db_username       = "workorder"
  db_password       = var.db_password
  bucket_name       = local.persistent.files_bucket
  bucket_arn        = local.persistent.files_bucket_arn
  spring_profile      = "staging"
  jwt_secret_ssm_name = "/vatika-work-order/staging/jwt-secret"
}

resource "aws_security_group_rule" "rds_from_ecs" {
  type                     = "ingress"
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
  security_group_id        = local.persistent.db_sg_id
  source_security_group_id = local.persistent.app_sg_id
}

module "cloudwatch" {
  source           = "../../modules/cloudwatch"
  name             = var.name
  region           = var.region
  namespace        = "WorkOrderPlatform"
  alb_arn_suffix   = module.ecs.alb_arn_suffix
  tg_arn_suffix    = module.ecs.tg_arn_suffix
  ecs_cluster_name = module.ecs.cluster_name
  ecs_service_name = module.ecs.service_name
  rds_instance_id  = local.persistent.rds_instance_id
  log_group        = module.ecs.log_group_name
}