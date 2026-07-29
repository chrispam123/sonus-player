# ============================================================
# DYNAMODB.TF — Tablas de base de datos NoSQL
# ============================================================
# DynamoDB es la base de datos serverless de AWS.
# No hay servidor que mantener — pagas solo por lo que usas.
#
# Creamos 3 tablas:
#   1. lyrics_cache    → Caché de letras generadas por DeepSeek
#   2. request_status  → Estado de solicitudes asíncronas (para polling)
#   3. mood_history    → Historial de análisis de mood por usuario
#
# Modo de facturación: PAY_PER_REQUEST
#   → Solo pagas por cada lectura/escritura
#   → Ideal para tráfico variable (0 costo cuando nadie usa la app)
# ============================================================

# ============================================================
# TABLA 1: Caché de letras
# ============================================================
# Guarda las letras generadas por DeepSeek.
# Si se pide la misma canción dos veces, la segunda vez
# se sirve desde aquí sin llamar a DeepSeek (ahorra costos).
#
# Estructura:
#   PK: "bad_bunny#titi_me_pregunto"  (artista#titulo normalizado)
#   lyrics: "texto completo de la letra..."
#   source: "deepseek"
#   ttl: epoch timestamp (DynamoDB borra automáticamente al expirar)
# ============================================================
resource "aws_dynamodb_table" "lyrics_cache" {
  name         = "${local.prefix}-lyrics-cache"
  billing_mode = "PAY_PER_REQUEST"  # Sin capacidad provisionada — escala automáticamente

  # Clave primaria: artista#titulo normalizado
  hash_key = "pk"

  attribute {
    name = "pk"
    type = "S"  # S = String (también hay N = Number, B = Binary)
  }

  # TTL: DynamoDB lee el campo "ttl" y borra el item automáticamente
  # cuando el timestamp expira. Esto mantiene la tabla limpia sin
  # necesidad de un cron job de limpieza.
  ttl {
    attribute_name = "ttl"
    enabled        = true
  }

  tags = {
    Name = "${local.prefix}-lyrics-cache"
  }
}

# ============================================================
# TABLA 2: Estado de solicitudes asíncronas
# ============================================================
# Cuando la app hace POST /lyrics o POST /mood, el Receptor
# devuelve un requestId y guarda status = PENDING aquí.
# Cuando el Processor termina, actualiza a COMPLETED + datos.
# La app hace polling con GET /result/{requestId} hasta que
# el status cambia.
#
# Estructura:
#   PK: "abc-123-uuid"  (el requestId)
#   status: "PENDING" | "COMPLETED" | "FAILED"
#   data: "{ JSON del resultado }"  (solo cuando COMPLETED)
#   error: "mensaje de error"  (solo cuando FAILED)
#   ttl: expira en 24 horas (el polling no dura más)
# ============================================================
resource "aws_dynamodb_table" "request_status" {
  name         = "${local.prefix}-request-status"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "pk"

  attribute {
    name = "pk"
    type = "S"
  }

  ttl {
    attribute_name = "ttl"
    enabled        = true
  }

  tags = {
    Name = "${local.prefix}-request-status"
  }
}

# ============================================================
# TABLA 3: Historial de mood por usuario
# ============================================================
# Guarda el historial de análisis de mood del usuario.
# Usa esquema PK + SK para poder consultar todo el historial
# de un usuario ordenado por tiempo.
#
# Estructura:
#   PK: "user_1"  (ID del usuario — fijo para app personal)
#   SK: 1720000000  (timestamp epoch — ordena cronológicamente)
#   mood: "melancholy"
#   shaderMood: "MOIRE_FLOW"
#   description: "Pareces estar en..."
#   ttl: expira en 90 días
# ============================================================
resource "aws_dynamodb_table" "mood_history" {
  name         = "${local.prefix}-mood-history"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "pk"   # userId
  range_key    = "sk"   # timestamp — permite sort y range queries

  attribute {
    name = "pk"
    type = "S"  # userId
  }

  attribute {
    name = "sk"
    type = "N"  # timestamp numérico
  }

  ttl {
    attribute_name = "ttl"
    enabled        = true
  }

  tags = {
    Name = "${local.prefix}-mood-history"
  }
}
