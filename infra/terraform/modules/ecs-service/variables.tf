variable "name"              { type = string }
variable "region"            { type = string }
variable "image"             { type = string }
variable "subnet_ids"        { type = list(string) }
variable "security_group_id" { type = string }
variable "db_endpoint"       { type = string }
variable "db_name"           { type = string }
variable "db_username"       { type = string }
variable "db_password" {
  type      = string
  sensitive = true
}
variable "bucket_name"       { type = string }
variable "bucket_arn"        { type = string }