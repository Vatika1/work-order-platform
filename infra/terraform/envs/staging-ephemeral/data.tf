data "terraform_remote_state" "persistent" {
  backend = "s3"
  config = {
    bucket = "vatika-work-order-tfstate"
    key    = "staging/persistent/terraform.tfstate"
    region = "ca-central-1"
  }
}