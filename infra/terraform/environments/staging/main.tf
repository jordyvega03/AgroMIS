terraform {
  required_version = ">= 1.7"
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 5.0" }
    tls = { source = "hashicorp/tls", version = "~> 4.0" }
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = local.common_tags
  }
}

locals {
  env         = "staging"
  name_prefix = "agromis-${local.env}"
  common_tags = {
    Project     = "agromis"
    Environment = local.env
    ManagedBy   = "terraform"
  }
}

module "vpc" {
  source      = "../../modules/vpc"
  name_prefix = local.name_prefix
  vpc_cidr    = "10.0.0.0/16"
  common_tags = local.common_tags
}

module "eks" {
  source             = "../../modules/eks"
  name_prefix        = local.name_prefix
  private_subnet_ids = module.vpc.private_subnet_ids
  common_tags        = local.common_tags
}

module "rds" {
  source               = "../../modules/rds-postgres"
  name_prefix          = local.name_prefix
  vpc_id               = module.vpc.vpc_id
  isolated_subnet_ids  = module.vpc.isolated_subnet_ids
  eks_node_sg_id       = module.eks.node_role_arn
  db_password          = var.db_password
  instance_class       = "db.t4g.medium"
  allocated_storage    = 100
  multi_az             = false
  deletion_protection  = false
  backup_retention_days = 7
  common_tags          = local.common_tags
}

module "redis" {
  source               = "../../modules/elasticache-redis"
  name_prefix          = local.name_prefix
  vpc_id               = module.vpc.vpc_id
  isolated_subnet_ids  = module.vpc.isolated_subnet_ids
  eks_node_sg_id       = module.eks.node_role_arn
  node_type            = "cache.t4g.small"
  common_tags          = local.common_tags
}

module "s3" {
  source      = "../../modules/s3-datalake"
  name_prefix = local.env
  common_tags = local.common_tags
}
