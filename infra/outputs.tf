# ============================================================
# OUTPUTS.TF — Valores que Terraform imprime al finalizar
# ============================================================
# Después de "terraform apply", estos valores se muestran
# en la terminal. Son los datos que la app Android necesita
# para conectarse al backend.
#
# También se pueden consultar después con:
#   terraform output api_url
#   terraform output -json
# ============================================================

output "api_url" {
  description = "URL base del API Gateway. La app Android usa esta URL para todas las llamadas."
  value       = "${aws_apigatewayv2_stage.default.invoke_url}"
  # Ejemplo: https://abc123.execute-api.us-east-1.amazonaws.com/develop
}

output "lyrics_endpoint" {
  description = "Endpoint completo para solicitar letras de canciones."
  value       = "${aws_apigatewayv2_stage.default.invoke_url}/lyrics"
}

output "mood_endpoint" {
  description = "Endpoint completo para análisis de mood."
  value       = "${aws_apigatewayv2_stage.default.invoke_url}/mood"
}

output "result_endpoint" {
  description = "Endpoint completo para polling de resultados. Reemplaza {requestId} con el UUID."
  value       = "${aws_apigatewayv2_stage.default.invoke_url}/result/{requestId}"
}

output "lyrics_queue_url" {
  description = "URL de la cola SQS de letras (sonus-develop-sqs-lyrics)."
  value       = aws_sqs_queue.lyrics_queue.url
}

output "mood_queue_url" {
  description = "URL de la cola SQS de mood (sonus-develop-sqs-mood)."
  value       = aws_sqs_queue.mood_queue.url
}

output "environment" {
  description = "Entorno desplegado."
  value       = var.environment
}

output "region" {
  description = "Región AWS donde está desplegado."
  value       = var.aws_region
}

# 🆕 API Key — se envía en el header x-api-key desde la app Android
output "api_key_value" {
  description = "Valor de la API Key para autenticar requests."
  value       = aws_apigatewayv2_api_key.sonus_app.value
  sensitive   = true
}
