# ============================================================
# VARIABLES — Parámetros configurables de la infraestructura
# ============================================================
# Estas variables permiten reutilizar el mismo código Terraform
#para diferentes entornos (develop, prod) solo cambiando los valores.
#
# Uso:
#   terraform apply -var-file="environments/develop.tfvars"
#   terraform apply -var-file="environments/prod.tfvars"
# ============================================================

variable "aws_region" {
  description = "Región de AWS donde se despliega toda la infraestructura"
  type        = string
  default     = "us-east-1"  # N. Virginia — buena latencia para América
}

variable "environment" {
  description = "Nombre del entorno (develop o prod). Se usa como prefijo en todos los recursos."
  type        = string
  default     = "develop"
  # Validación: solo permite "develop" o "prod"
  validation {
    condition     = contains(["develop", "prod"], var.environment)
    error_message = "environment debe ser 'develop' o 'prod'."
  }
}

variable "project_name" {
  description = "Nombre del proyecto. Se usa como prefijo en todos los recursos AWS."
  type        = string
  default     = "sonus"
}

variable "deepseek_api_key" {
  description = "API key de DeepSeek para generar letras y analizar moods. SENSIBLE — no commitear."
  type        = string
  sensitive   = true  # Terraform no mostrará este valor en los logs
}

variable "lambda_timeout_seconds" {
  description = "Tiempo máximo de ejecución de las Lambdas en segundos."
  type        = number
  default     = 60   # 60s — suficiente para DeepSeek (máx ~15-20s)
}

variable "lambda_memory_mb" {
  description = "Memoria RAM asignada a las Lambdas en MB. Más memoria = más CPU también en Lambda."
  type        = number
  default     = 512  # 512MB — balance entre costo y velocidad para JVM
}

variable "sqs_message_retention_seconds" {
  description = "Cuántos segundos SQS retiene mensajes no procesados antes de descartarlos."
  type        = number
  default     = 86400  # 24 horas — si Lambda está caída, SQS guarda los mensajes
}

variable "sqs_max_receive_count" {
  description = "Cuántas veces puede intentarse procesar un mensaje antes de enviarlo al DLQ."
  type        = number
  default     = 3  # 3 intentos antes de mover al Dead Letter Queue
}
