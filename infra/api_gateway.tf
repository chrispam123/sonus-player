# ============================================================
# API_GATEWAY.TF — Endpoints HTTP públicos
# ============================================================
# API Gateway expone las Lambdas como endpoints HTTP.
# La app Android llama a estas URLs.
#
# Creamos 3 endpoints:
#   POST /lyrics    → Lambda Receptor
#   POST /mood      → Lambda Receptor
#   GET  /result/{requestId} → Lambda Receptor
#
# Usamos HTTP API (v2) — más simple y barato que REST API (v1).
# HTTP API cuesta ~70% menos y tiene menos latencia.
#
# Nomenclatura: sonus-develop-apigateway
# ============================================================

# ============================================================
# HTTP API — El gateway principal
# ============================================================
resource "aws_apigatewayv2_api" "sonus_api" {
  # Nombre: sonus-develop-apigateway
  name          = "${local.prefix}-apigateway"
  protocol_type = "HTTP"

  # CORS — necesario para que la app Android pueda llamar a la API
  # 🆕 x-api-key agregado para autenticación
  cors_configuration {
    allow_headers = ["Content-Type", "Authorization", "x-api-key"]
    allow_methods = ["GET", "POST", "OPTIONS"]
    allow_origins = ["*"]
  }

  tags = {
    Name = "${local.prefix}-apigateway"
  }
}

# ============================================================
# STAGE — Entorno de despliegue (develop o prod)
# ============================================================
# El stage se agrega a la URL: https://xxx.execute-api.us-east-1.amazonaws.com/develop/
# Usando var.environment, la URL cambia automáticamente entre entornos.
resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.sonus_api.id
  # Stage name: "develop" o "prod" según el environment variable
  name        = var.environment
  auto_deploy = true  # Desplegar automáticamente cuando cambia la API

  # Logging de acceso en CloudWatch — para ver qué requests llegan
  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.api_gateway_logs.arn
    format = jsonencode({
      requestId      = "$context.requestId"
      ip             = "$context.identity.sourceIp"
      requestTime    = "$context.requestTime"
      httpMethod     = "$context.httpMethod"
      path           = "$context.path"
      status         = "$context.status"
      responseLength = "$context.responseLength"
      integrationLatency = "$context.integrationLatency"
    })
  }

  # 🆕 Throttling: protege contra abuso incluso con API Key extraída.
  # Máximo 10 requests en ráfaga, 5 requests/segundo sostenido.
  default_route_settings {
    throttling_burst_limit = 10
    throttling_rate_limit  = 5
  }

  tags = {
    Name = "${local.prefix}-apigateway-stage"
  }
}

# Grupo de logs de CloudWatch para API Gateway
resource "aws_cloudwatch_log_group" "api_gateway_logs" {
  # Nombre: sonus-develop-apigateway-logs
  name              = "/aws/apigateway/${local.prefix}"
  retention_in_days = 7  # Guardar logs 7 días (ajustable)
}

# ============================================================
# INTEGRACIÓN: API Gateway → Lambda Receptor
# ============================================================
# Conecta el API Gateway con la Lambda Receptor.
# Todas las rutas usan la misma Lambda (el Receptor enruta internamente).
resource "aws_apigatewayv2_integration" "receptor_integration" {
  api_id             = aws_apigatewayv2_api.sonus_api.id
  integration_type   = "AWS_PROXY"  # Lambda proxy — pasa el request completo
  integration_uri    = aws_lambda_function.receptor.invoke_arn
  payload_format_version = "2.0"    # Formato moderno de HTTP API
}

# ============================================================
# RUTAS — Los endpoints disponibles
# ============================================================

# POST /lyrics — Solicitar letra de una canción
resource "aws_apigatewayv2_route" "post_lyrics" {
  api_id    = aws_apigatewayv2_api.sonus_api.id
  route_key = "POST /lyrics"
  target    = "integrations/${aws_apigatewayv2_integration.receptor_integration.id}"
  api_key_required = true  # 🆕 Requiere API Key
}

# POST /mood — Enviar historial para análisis de mood
resource "aws_apigatewayv2_route" "post_mood" {
  api_id    = aws_apigatewayv2_api.sonus_api.id
  route_key = "POST /mood"
  target    = "integrations/${aws_apigatewayv2_integration.receptor_integration.id}"
  api_key_required = true  # 🆕 Requiere API Key
}

# GET /result/{requestId} — Consultar estado de una solicitud
resource "aws_apigatewayv2_route" "get_result" {
  api_id    = aws_apigatewayv2_api.sonus_api.id
  route_key = "GET /result/{requestId}"
  target    = "integrations/${aws_apigatewayv2_integration.receptor_integration.id}"
  api_key_required = true  # 🆕 Requiere API Key
}

# ============================================================
# PERMISO: API Gateway puede invocar Lambda Receptor
# ============================================================
# Sin este permiso, API Gateway no puede llamar a la Lambda
# aunque estén configurados en el mismo AWS account.
resource "aws_lambda_permission" "api_gateway_receptor" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.receptor.function_name
  principal     = "apigateway.amazonaws.com"

  # Restricción: solo este API Gateway puede invocar esta Lambda
  source_arn = "${aws_apigatewayv2_api.sonus_api.execution_arn}/*/*"
}

# ============================================================
# 🆕 SEGURIDAD: API Key + Usage Plan + Throttling
# ============================================================
# Capa 1: API Key — solo requests con key válida pasan.
# Frena scrapers genéricos y bots. La app Android la envía en
# el header x-api-key.
# ============================================================

resource "aws_apigatewayv2_api_key" "sonus_app" {
  name        = "${local.prefix}-app-key"
  description = "API Key para la app Android Sonus (${local.prefix})"
  tags = { Name = "${local.prefix}-app-key" }
}

# Usage Plan — controla cuántos requests puede hacer la API Key
resource "aws_apigatewayv2_usage_plan" "sonus_plan" {
  name        = "${local.prefix}-usage-plan"
  description = "Usage plan con throttling para Sonus (${local.prefix})"
  api_stages {
    api_id = aws_apigatewayv2_api.sonus_api.id
    stage  = aws_apigatewayv2_stage.default.id
  }
}

# Vincular la API Key al Usage Plan
resource "aws_apigatewayv2_usage_plan_key" "sonus_plan_key" {
  api_key_id    = aws_apigatewayv2_api_key.sonus_app.id
  key_type      = "API_KEY"
  usage_plan_id = aws_apigatewayv2_usage_plan.sonus_plan.id
}
