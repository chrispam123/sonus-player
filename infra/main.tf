# ============================================================
# MAIN.TF — Provider AWS y locals
# ============================================================
# NOTA: El backend (estado remoto S3) está en backend.tf.
# Este archivo solo configura el provider y los locals.
# ============================================================

# Provider de AWS
# Las credenciales se toman de ~/.aws/credentials
# (ya configuradas con: aws configure)
provider "aws" {
  region = var.aws_region

  # Tags aplicados automáticamente a TODOS los recursos de AWS
  # Muy útil para filtrar costos en AWS Cost Explorer
  # y saber qué pertenece a qué proyecto/entorno
  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "terraform"
      Repository  = "sonus-player"
    }
  }
}

# ============================================================
# LOCALS — Valores derivados reutilizables
# ============================================================
# En vez de repetir "${var.project_name}-${var.environment}"
# en cada recurso, lo calculamos una vez aquí.
#
# Con environment = "develop" y project_name = "sonus":
#   local.prefix = "sonus-develop"
#
# Resultado en los recursos:
#   sonus-develop-apigateway
#   sonus-develop-sqs-lyrics
#   sonus-develop-lambda-receptor
#   etc.
# ============================================================
locals {
  prefix = "${var.project_name}-${var.environment}"
}
