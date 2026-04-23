variable "alert_emails" {
  type        = list(string)
  description = "Lista de emails que reciben alertas de presupuesto"
  default     = ["sre@agromis.io"]
}
