variable "name_prefix"        { type = string }
variable "vpc_id"             { type = string }
variable "private_subnet_id"  { type = string }
variable "common_tags"        { type = map(string); default = {} }
