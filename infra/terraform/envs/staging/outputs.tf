output "ecr_repository_url" { value = module.ecr.repository_url }
output "db_endpoint"        { value = module.rds.endpoint }
output "files_bucket"       { value = module.s3.bucket_name }
output "github_deploy_role_arn" { value = module.github_oidc.role_arn }