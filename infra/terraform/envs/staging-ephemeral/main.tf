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
  image             = "${local.persistent.ecr_repository_url}:${var.image_tag}"
  subnet_ids        = local.persistent.public_subnet_ids
  security_group_id = local.persistent.app_sg_id
  db_endpoint       = local.persistent.db_endpoint
  db_name           = "workorder"
  db_username       = "workorder"
  db_password       = var.db_password
  bucket_name       = local.persistent.files_bucket
  bucket_arn        = local.persistent.files_bucket_arn
}

resource "aws_security_group_rule" "rds_from_ecs" {
  type                     = "ingress"
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
  security_group_id        = local.persistent.db_sg_id
  source_security_group_id = local.persistent.app_sg_id
}