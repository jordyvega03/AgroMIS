variable "name_prefix"          { type = string }
variable "private_subnet_ids"   { type = list(string) }
variable "kubernetes_version"   { type = string; default = "1.29" }
variable "core_instance_type"   { type = string; default = "t3.large" }
variable "batch_instance_type"  { type = string; default = "t3.medium" }
variable "core_desired"         { type = number; default = 2 }
variable "core_min"             { type = number; default = 2 }
variable "core_max"             { type = number; default = 6 }
variable "common_tags"          { type = map(string); default = {} }
