terraform {
  backend "s3" {
    bucket         = "agromis-terraform-state-staging"
    key            = "staging/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "agromis-terraform-locks-staging"
    encrypt        = true
  }
}
