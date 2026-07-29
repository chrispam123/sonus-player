# ============================================================
# PROD.TFVARS — Variables para entorno de producción
# ============================================================
# Usar con: terraform apply -var-file="environments/prod.tfvars"
#
# Todos los recursos llevan el prefijo "sonus-prod-".
# Mayor memoria y timeout para mayor estabilidad.
# ============================================================

aws_region   = "us-east-1"
environment  = "prod"
project_name = "sonus"

# Más recursos en prod para mejor rendimiento
lambda_memory_mb       = 1024  # 1GB — cold starts más rápidos en JVM
lambda_timeout_seconds = 60

sqs_message_retention_seconds = 86400
sqs_max_receive_count         = 3
