variable "name_prefix" {
  type        = string
  description = "Prefijo para todos los recursos (ej. agromis-staging)"
}

variable "vpc_cidr" {
  type        = string
  description = "CIDR block de la VPC"
  default     = "10.0.0.0/16"
}

variable "common_tags" {
  type        = map(string)
  description = "Tags comunes aplicados a todos los recursos"
  default     = {}
}
