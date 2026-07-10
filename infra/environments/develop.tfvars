# ============================================================
# DEVELOP.TFVARS — Variables para entorno de desarrollo
# ============================================================
# Usar con: terraform apply -var-file="environments/develop.tfvars"
#
# Este entorno es para pruebas. Todos los recursos llevan
# el prefijo "sonus-develop-" para distinguirlos de producción.
# ============================================================

aws_region   = "us-east-1"
environment  = "develop"
project_name = "sonus"

# Recursos más pequeños en dev para ahorrar costos
lambda_memory_mb       = 512
lambda_timeout_seconds = 60

sqs_message_retention_seconds = 86400  # 24 horas
sqs_max_receive_count         = 3

# deepseek_api_key se pasa como variable de entorno o en CLI:
# terraform apply -var-file="environments/develop.tfvars" -var="deepseek_api_key=sk-f9b5eb1fb2724f08aec6d5811c457836"
