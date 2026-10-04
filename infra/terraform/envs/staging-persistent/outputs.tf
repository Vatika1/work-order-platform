# Read by envs/staging-ephemeral via data.terraform_remote_state.persistent
output "public_subnet_ids"  { value = module.network.public_subnet_ids }
output "app_sg_id"          { value = module.network.app_sg_id }
output "db_sg_id"           { value = module.network.db_sg_id }
output "ecr_repository_url" { value = module.ecr.repository_url }
output "ecr_repository_arn" { value = module.ecr.repository_arn }
output "db_endpoint"        { value = module.rds.endpoint }
output "files_bucket"       { value = module.s3.bucket_name }
output "files_bucket_arn"   { value = module.s3.bucket_arn }
output "github_deploy_role_arn" { value = module.github_oidc.role_arn }