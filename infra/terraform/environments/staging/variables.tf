variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "db_password" {
  type      = string
  sensitive = true
  description = "Password para la instancia RDS. Pasar via TF_VAR_db_password o secrets manager."
}
