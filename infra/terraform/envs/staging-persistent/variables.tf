variable "name" {
  type    = string
  default = "vatika-work-order-staging"
}

variable "region" {
  type    = string
  default = "ca-central-1"
}

variable "db_password" {
  type      = string
  sensitive = true
}