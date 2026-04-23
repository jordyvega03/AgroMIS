variable "name_prefix"    { type = string }
variable "kms_key_arn"   { type = string }
variable "retention_days" { type = number; default = 30 }
variable "common_tags"   { type = map(string); default = {} }
