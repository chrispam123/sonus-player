# ============================================================
# IAM.TF — Roles y permisos de AWS
# ============================================================
# IAM (Identity and Access Management) controla quién puede
# hacer qué en AWS. Seguimos el principio de mínimo privilegio:
# cada Lambda solo tiene permisos para lo que necesita.
#
# Estructura:
#   1. IAM Role     → "Quién soy" (identidad de la Lambda)
#   2. IAM Policy   → "Qué puedo hacer" (permisos específicos)
#   3. Attachment   → "Conecto la identidad con los permisos"
#
# Creamos un rol separado para cada Lambda porque tienen
# necesidades diferentes:
#   Receptor  → lee DynamoDB + escribe SQS
#   Lyrics    → lee SQS + escribe DynamoDB
#   Mood      → lee SQS + escribe DynamoDB (tablas diferentes)
# ============================================================

# ============================================================
# ROL BASE: Permite a Lambda ejecutarse
# ============================================================
# Todas las Lambdas necesitan este rol mínimo para:
#   - Crear logs en CloudWatch (para debugging)
#   - Ejecutarse como función Lambda
#
# El "assume_role_policy" dice "el servicio Lambda puede asumir este rol"
data "aws_iam_policy_document" "lambda_assume_role" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

# ============================================================
# LAMBDA RECEPTOR — Rol e Permisos
# ============================================================
resource "aws_iam_role" "lambda_receptor" {
  name               = "${local.prefix}-lambda-receptor-role"
  assume_role_policy = data.aws_iam_policy_document.lambda_assume_role.json
}

# Permisos del Receptor:
#   - Escribir logs en CloudWatch (debugging)
#   - Leer y escribir en DynamoDB (cache + request status)
#   - Enviar mensajes a SQS (encolar solicitudes)
resource "aws_iam_role_policy" "receptor_policy" {
  name = "${local.prefix}-receptor-policy"
  role = aws_iam_role.lambda_receptor.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      # Logs de CloudWatch — para ver qué hace la Lambda
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "arn:aws:logs:*:*:*"
      },
      # DynamoDB — leer caché y gestionar estados de requests
      {
        Effect = "Allow"
        Action = [
          "dynamodb:GetItem",      # Leer una letra del caché
          "dynamodb:PutItem",      # Guardar estado PENDING
          "dynamodb:UpdateItem",   # No necesario para Receptor, incluido por completitud
          "dynamodb:Query"         # Buscar por PK
        ]
        Resource = [
          aws_dynamodb_table.lyrics_cache.arn,
          aws_dynamodb_table.request_status.arn
        ]
      },
      # SQS — enviar mensajes a las colas de procesamiento
      {
        Effect = "Allow"
        Action = ["sqs:SendMessage"]
        Resource = [
          aws_sqs_queue.lyrics_queue.arn,
          aws_sqs_queue.mood_queue.arn
        ]
      }
    ]
  })
}

# ============================================================
# LAMBDA LYRICS — Rol y Permisos
# ============================================================
resource "aws_iam_role" "lambda_lyrics" {
  name               = "${local.prefix}-lambda-lyrics-role"
  assume_role_policy = data.aws_iam_policy_document.lambda_assume_role.json
}

resource "aws_iam_role_policy" "lyrics_policy" {
  name = "${local.prefix}-lyrics-policy"
  role = aws_iam_role.lambda_lyrics.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      # CloudWatch Logs
      {
        Effect = "Allow"
        Action = ["logs:CreateLogGroup", "logs:CreateLogStream", "logs:PutLogEvents"]
        Resource = "arn:aws:logs:*:*:*"
      },
      # SQS — leer y borrar mensajes de la cola de letras
      # ReceiveMessage: leer mensajes
      # DeleteMessage: borrar después de procesar exitosamente
      # GetQueueAttributes: necesario para el event source mapping
      {
        Effect = "Allow"
        Action = [
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes"
        ]
        Resource = [
          aws_sqs_queue.lyrics_queue.arn,
          aws_sqs_queue.lyrics_dlq.arn
        ]
      },
      # DynamoDB — guardar letras en caché y actualizar estados
      {
        Effect = "Allow"
        Action = ["dynamodb:PutItem", "dynamodb:UpdateItem", "dynamodb:GetItem"]
        Resource = [
          aws_dynamodb_table.lyrics_cache.arn,
          aws_dynamodb_table.request_status.arn
        ]
      }
    ]
  })
}

# ============================================================
# LAMBDA MOOD — Rol y Permisos
# ============================================================
resource "aws_iam_role" "lambda_mood" {
  name               = "${local.prefix}-lambda-mood-role"
  assume_role_policy = data.aws_iam_policy_document.lambda_assume_role.json
}

resource "aws_iam_role_policy" "mood_policy" {
  name = "${local.prefix}-mood-policy"
  role = aws_iam_role.lambda_mood.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      # CloudWatch Logs
      {
        Effect = "Allow"
        Action = ["logs:CreateLogGroup", "logs:CreateLogStream", "logs:PutLogEvents"]
        Resource = "arn:aws:logs:*:*:*"
      },
      # SQS — leer mensajes de la cola de mood
      {
        Effect = "Allow"
        Action = ["sqs:ReceiveMessage", "sqs:DeleteMessage", "sqs:GetQueueAttributes"]
        Resource = [
          aws_sqs_queue.mood_queue.arn,
          aws_sqs_queue.mood_dlq.arn
        ]
      },
      # DynamoDB — guardar historial de mood y actualizar estados
      {
        Effect = "Allow"
        Action = ["dynamodb:PutItem", "dynamodb:UpdateItem"]
        Resource = [
          aws_dynamodb_table.request_status.arn,
          aws_dynamodb_table.mood_history.arn
        ]
      }
    ]
  })
}
