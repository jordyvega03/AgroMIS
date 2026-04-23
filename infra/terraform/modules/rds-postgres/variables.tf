variable "name_prefix"           { type = string }
variable "vpc_id"                { type = string }
variable "isolated_subnet_ids"   { type = list(string) }
variable "eks_node_sg_id"        { type = string }
variable "db_password"           { type = string; sensitive = true }
variable "instance_class"        { type = string; default = "db.t4g.medium" }
variable "allocated_storage"     { type = number; default = 100 }
variable "multi_az"              { type = bool;   default = false }
variable "deletion_protection"   { type = bool;   default = false }
variable "backup_retention_days" { type = number; default = 7 }
variable "common_tags"           { type = map(string); default = {} }
