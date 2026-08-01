# ============================================================
# LAMBDA.TF — Funciones Lambda
# ============================================================
# Define las 3 funciones Lambda con:
#   - El JAR de Kotlin compilado (el "código" de la Lambda)
#   - Variables de entorno (URLs de colas, tablas, API keys)
#   - El IAM role (permisos)
#   - El trigger de SQS (qué dispara cada Lambda)
#
# IMPORTANTE: Antes de hacer "terraform apply" debes compilar
# los JARs de Kotlin:
#   cd backend && ./gradlew :lambda-receptor:shadowJar
#   cd backend && ./gradlew :lambda-lyrics:shadowJar
#   cd backend && ./gradlew :lambda-mood:shadowJar
#
# Los JARs se generan en:
#   backend/lambda-receptor/build/libs/lambda-receptor.jar
#   backend/lambda-lyrics/build/libs/lambda-lyrics.jar
#   backend/lambda-mood/build/libs/lambda-mood.jar
# ============================================================

# ============================================================
# LAMBDA 1: Receptor (entrada HTTP via API Gateway)
# ============================================================
resource "aws_lambda_function" "receptor" {
  function_name = "${local.prefix}-receptor"

  # Ruta al JAR compilado — relativa al directorio infra/
  filename = "../backend/lambda-receptor/build/libs/lambda-receptor.jar"

  # Hash del JAR — Terraform actualiza la Lambda cuando el JAR cambia
  source_code_hash = filebase64sha256("../backend/lambda-receptor/build/libs/lambda-receptor.jar")

  # El handler es la clase Java que implementa RequestHandler
  # Formato: paquete.completo.NombreClase::handleRequest
  handler = "com.sonus.receptor.Handler::handleRequest"

  # Runtime Java 17 — coincide con el jvmToolchain(17) de Gradle
  runtime = "java17"

  # IAM role con los permisos necesarios (definido en iam.tf)
  role = aws_iam_role.lambda_receptor.arn

  timeout     = var.lambda_timeout_seconds  # 60 segundos
  memory_size = var.lambda_memory_mb        # 512 MB

  # Variables de entorno que la Lambda puede leer con System.getenv()
  # JAVA_TOOL_OPTIONS: configuración JVM optimizada para cold starts
  # -XX:TieredCompilation reduce el tiempo de JIT compilation
  # -XX:+UseSerialGC usa un GC simple (menos memoria para workloads cortos)
  environment {
    variables = {
      JAVA_TOOL_OPTIONS    = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:+UseSerialGC"

      # Nombres de las tablas DynamoDB
      LYRICS_CACHE_TABLE   = aws_dynamodb_table.lyrics_cache.name
      REQUEST_STATUS_TABLE = aws_dynamodb_table.request_status.name

      # URLs de las colas SQS
      LYRICS_QUEUE_URL = aws_sqs_queue.lyrics_queue.url
      MOOD_QUEUE_URL   = aws_sqs_queue.mood_queue.url

      # Entorno actual (útil para logging)
      ENVIRONMENT = var.environment

      # 🆕 API Key para autenticación de la app Android
      SONUS_API_KEY = random_password.sonus_api_key.result
    }
  }

  tags = {
    Name = "${local.prefix}-receptor"
  }
}

# ============================================================
# LAMBDA 2: Lyrics Processor (disparada por SQS lyrics_queue)
# ============================================================
resource "aws_lambda_function" "lyrics_processor" {
  function_name    = "${local.prefix}-lyrics-processor"
  filename         = "../backend/lambda-lyrics/build/libs/lambda-lyrics.jar"
  source_code_hash = filebase64sha256("../backend/lambda-lyrics/build/libs/lambda-lyrics.jar")
  handler          = "com.sonus.lyrics.Handler::handleRequest"
  runtime          = "java17"
  role             = aws_iam_role.lambda_lyrics.arn
  timeout          = var.lambda_timeout_seconds
  memory_size      = var.lambda_memory_mb

  environment {
    variables = {
      JAVA_TOOL_OPTIONS    = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:+UseSerialGC"
      LYRICS_CACHE_TABLE   = aws_dynamodb_table.lyrics_cache.name
      REQUEST_STATUS_TABLE = aws_dynamodb_table.request_status.name
      DEEPSEEK_API_KEY     = var.deepseek_api_key  # API key de DeepSeek
      ENVIRONMENT          = var.environment
    }
  }

  tags = {
    Name = "${local.prefix}-lyrics-processor"
  }
}

# ============================================================
# TRIGGER: SQS lyrics_queue → Lambda Lyrics Processor
# ============================================================
# Este "event source mapping" hace que Lambda se dispare
# automáticamente cuando hay mensajes en la cola.
# batch_size = 10: procesa hasta 10 mensajes por invocación
resource "aws_lambda_event_source_mapping" "lyrics_trigger" {
  event_source_arn = aws_sqs_queue.lyrics_queue.arn
  function_name    = aws_lambda_function.lyrics_processor.arn
  batch_size       = 5  # Procesar hasta 5 letras por invocación
  enabled          = true
}

# ============================================================
# LAMBDA 3: Mood Analyzer (disparada por SQS mood_queue)
# ============================================================
resource "aws_lambda_function" "mood_analyzer" {
  function_name    = "${local.prefix}-mood-analyzer"
  filename         = "../backend/lambda-mood/build/libs/lambda-mood.jar"
  source_code_hash = filebase64sha256("../backend/lambda-mood/build/libs/lambda-mood.jar")
  handler          = "com.sonus.mood.Handler::handleRequest"
  runtime          = "java17"
  role             = aws_iam_role.lambda_mood.arn
  timeout          = var.lambda_timeout_seconds
  memory_size      = var.lambda_memory_mb

  environment {
    variables = {
      JAVA_TOOL_OPTIONS    = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:+UseSerialGC"
      REQUEST_STATUS_TABLE = aws_dynamodb_table.request_status.name
      MOOD_HISTORY_TABLE   = aws_dynamodb_table.mood_history.name
      DEEPSEEK_API_KEY     = var.deepseek_api_key
      ENVIRONMENT          = var.environment
    }
  }

  tags = {
    Name = "${local.prefix}-mood-analyzer"
  }
}

# ============================================================
# TRIGGER: SQS mood_queue → Lambda Mood Analyzer
# ============================================================
resource "aws_lambda_event_source_mapping" "mood_trigger" {
  event_source_arn = aws_sqs_queue.mood_queue.arn
  function_name    = aws_lambda_function.mood_analyzer.arn
  batch_size       = 1   # Procesar 1 análisis de mood por vez (es más costoso)
  enabled          = true
}
