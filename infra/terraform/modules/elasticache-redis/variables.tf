variable "name_prefix"         { type = string }
variable "vpc_id"              { type = string }
variable "isolated_subnet_ids" { type = list(string) }
variable "eks_node_sg_id"      { type = string }
variable "node_type"           { type = string; default = "cache.t4g.small" }
variable "common_tags"         { type = map(string); default = {} }
