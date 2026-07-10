# ============================================================
# SQS.TF — Colas de mensajes (arquitectura EDA)
# ============================================================
# SQS (Simple Queue Service) es el corazón de nuestra
# arquitectura orientada a eventos (EDA).
#
# ¿Por qué SQS?
#   El Receptor responde en <200ms al cliente Android.
#   SQS guarda el trabajo pendiente.
#   Los Processors lo consumen cuando pueden.
#   Si un Processor falla → SQS reintenta automáticamente.
#   Si todo falla → el mensaje va al Dead Letter Queue (DLQ).
#
# Creamos 2 colas principales + 2 DLQs:
#   lyrics_queue    → mensajes de solicitudes de letras
#   lyrics_dlq      → letras que fallaron 3 veces (para inspección)
#   mood_queue      → mensajes de análisis de mood
#   mood_dlq        → moods que fallaron 3 veces
#
# ¿Qué es un DLQ (Dead Letter Queue)?
#   Si un mensaje falla N veces (maxReceiveCount), SQS lo mueve
#   al DLQ en vez de descartarlo. Puedes revisar el DLQ para
#   entender por qué ciertos mensajes no se pueden procesar.
# ============================================================

# ============================================================
# COLA PRINCIPAL: Lyrics Queue
# ============================================================
resource "aws_sqs_queue" "lyrics_queue" {
  name = "${local.prefix}-lyrics-queue"

  # Tiempo que SQS oculta un mensaje después de que Lambda lo recibe
  # Si Lambda no borra el mensaje en este tiempo, SQS lo vuelve visible
  # (para que otro worker lo reintente)
  # 300s = 5 minutos: suficiente para que DeepSeek genere una letra
  visibility_timeout_seconds = 300

  # Cuánto tiempo SQS retiene un mensaje si no se procesa
  message_retention_seconds = var.sqs_message_retention_seconds  # 24 horas

  # Configurar el DLQ como destino para mensajes que fallan repetidamente
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.lyrics_dlq.arn
    maxReceiveCount     = var.sqs_max_receive_count  # 3 intentos antes de DLQ
  })

  tags = {
    Name = "${local.prefix}-lyrics-queue"
  }
}

# ============================================================
# DEAD LETTER QUEUE: Lyrics DLQ
# ============================================================
# Mensajes que fallaron maxReceiveCount veces llegan aquí.
# Puedes inspeccionar estos mensajes en la consola de AWS
# para diagnosticar problemas (ej: canción en idioma no soportado)
resource "aws_sqs_queue" "lyrics_dlq" {
  name = "${local.prefix}-lyrics-dlq"

  # Retener mensajes del DLQ por 7 días para poder analizarlos
  message_retention_seconds = 604800  # 7 días

  tags = {
    Name = "${local.prefix}-lyrics-dlq"
  }
}

# ============================================================
# COLA PRINCIPAL: Mood Queue
# ============================================================
resource "aws_sqs_queue" "mood_queue" {
  name                       = "${local.prefix}-mood-queue"
  visibility_timeout_seconds = 300  # 5 minutos
  message_retention_seconds  = var.sqs_message_retention_seconds

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.mood_dlq.arn
    maxReceiveCount     = var.sqs_max_receive_count
  })

  tags = {
    Name = "${local.prefix}-mood-queue"
  }
}

# ============================================================
# DEAD LETTER QUEUE: Mood DLQ
# ============================================================
resource "aws_sqs_queue" "mood_dlq" {
  name                      = "${local.prefix}-mood-dlq"
  message_retention_seconds = 604800  # 7 días

  tags = {
    Name = "${local.prefix}-mood-dlq"
  }
}
