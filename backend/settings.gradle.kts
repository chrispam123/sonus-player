// ============================================================
// SONUS BACKEND — Gradle Settings
// ============================================================
// Este archivo registra los módulos del backend de Sonus.
// Cada módulo es una Lambda independiente + shared para tipos comunes.
//
// Módulos:
//   :shared         → Data classes compartidas entre Lambdas y Android
//   :lambda-receptor → Punto de entrada HTTP (API Gateway → SQS)
//   :lambda-lyrics   → Procesador de letras (SQS → DeepSeek → DynamoDB)
//   :lambda-mood     → Analizador de mood (SQS → DeepSeek → DynamoDB)
// ============================================================

rootProject.name = "sonus-backend"

include(":shared")
include(":lambda-receptor")
include(":lambda-lyrics")
include(":lambda-mood")
