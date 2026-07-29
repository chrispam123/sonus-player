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
# CAMBIO: Usamos terraform.workspace en vez de var.environment
# para que el prefijo cambie automáticamente según el workspace
# activo (develop o prod), aislando los estados en S3:
#   env:/develop/terraform.tfstate → sonus-develop-*
#   env:/prod/terraform.tfstate    → sonus-prod-*
#
# Con workspace = "develop" y project_name = "sonus":
#   local.prefix = "sonus-develop"
#
# Con workspace = "prod" y project_name = "sonus":
#   local.prefix = "sonus-prod"
#
# Resultado en los recursos:
#   sonus-develop-apigateway
#   sonus-develop-sqs-lyrics
#   sonus-develop-lambda-receptor
#   etc.
# ============================================================
locals {
  # terraform.workspace devuelve "develop" o "prod" según el workspace activo
  # Esto aísla automáticamente los recursos de cada entorno
  prefix = "${var.project_name}-${terraform.workspace}"
}
