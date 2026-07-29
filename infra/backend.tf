# ============================================================
# BACKEND.TF — Configuración del estado remoto de Terraform
# ============================================================
# El "backend" define DÓNDE guarda Terraform su archivo de estado.
#
# ¿Qué es el estado?
#   Un archivo JSON que registra todos los recursos que Terraform
#   creó en AWS. Sin el estado, Terraform no sabe qué existe
#   y puede crear duplicados o perder el control de los recursos.
#
# ¿Por qué S3 y no local?
#   - Local: el estado vive en tu máquina. Si la pierdes, pierdes
#     el control de todos los recursos en AWS.
#   - S3: el estado vive en AWS, siempre disponible, versionado,
#     encriptado. El CI/CD también puede acceder a él.
#
# EL PROBLEMA DEL "HUEVO Y LA GALLINA":
#   Este bucket S3 NO puede ser creado por Terraform (necesitaría
#   estado para rastrear su creación, pero el estado va en el bucket).
#   Por eso se crea MANUALMENTE UNA SOLA VEZ antes de terraform init.
#
# PASO 0 — Ejecutar UNA SOLA VEZ antes de cualquier terraform init:
# ============================================================
#
#   # 1. Crear el bucket S3 para el estado
#   aws s3 mb s3://sonus-terraform-state --region us-east-1
#
#   # 2. Activar versionado (recuperar estados anteriores si algo sale mal)
#   aws s3api put-bucket-versioning \
#     --bucket sonus-terraform-state \
#     --versioning-configuration Status=Enabled
#
#   # 3. Bloquear acceso público (el estado puede tener datos sensibles)
#   aws s3api put-public-access-block \
#     --bucket sonus-terraform-state \
#     --public-access-block-configuration \
#       "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"
#
# Después de el PASO 0, ya puedes usar:
#   terraform init
#   terraform plan  -var-file="environments/develop.tfvars"
#   terraform apply -var-file="environments/develop.tfvars"
# ============================================================

terraform {
  # Versión mínima de Terraform requerida
  # 1.10+ necesario para use_lockfile (locking nativo de S3)
  required_version = ">= 1.10.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Backend S3 — estado remoto compartido
  # NOTA: El bloque "backend" NO acepta variables (${var.xxx}).
  # Los valores deben ser literales. Por eso el nombre del bucket
  # está hardcodeado aquí (es el único lugar donde esto es aceptable).
  #
  # use_lockfile = true: Locking nativo de S3 (TF ≥ 1.10).
  # Usa S3 conditional writes (If-Match) para bloquear el estado.
  # Elimina la necesidad de una tabla DynamoDB separada para locks.
  backend "s3" {
    bucket       = "sonus-terraform-state"   # Bucket creado en PASO 0
    key          = "terraform.tfstate"       # Ruta del archivo de estado dentro del bucket
    region       = "us-east-1"
    encrypt      = true                      # Encriptar el estado (SSE-S3)
    use_lockfile = true                      # Locking nativo S3 — sin DynamoDB
  }
}
