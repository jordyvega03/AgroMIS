variable "name_prefix"           { type = string }
variable "service_name"          { type = string }
variable "oidc_provider_arn"     { type = string }
variable "oidc_provider_url"     { type = string }
variable "namespace"             { type = string }
variable "service_account_name"  { type = string }
variable "inline_policy"         { type = string; default = null }
variable "managed_policy_arns"   { type = list(string); default = [] }
variable "common_tags"           { type = map(string); default = {} }
