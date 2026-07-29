# 🎵 Sonus — AI-Powered Music Player

> Reproductor de música Android con inteligencia artificial que analiza tu estado de ánimo basándose en lo que escuchas.

[![Android](https://img.shields.io/badge/Android-10%2B-green)](https://developer.android.com) [![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue)](https://kotlinlang.org) [![AWS](https://img.shields.io/badge/AWS-Serverless-orange)](https://aws.amazon.com) [![Terraform](https://img.shields.io/badge/Terraform-1.10%2B-purple)](https://terraform.io)

---

## ✨ Features

| 🎧 | **Local + Streaming** — Reproduce tu biblioteca local y música Creative Commons de ccMixter |
| 🤖 | **Letras con IA** — DeepSeek genera letras para cualquier canción (incluso las más oscuras) |
| 🎭 | **Mood Analyzer** — Analiza tu historial de escucha y detecta tu estado de ánimo  y te da recomendaciones de canciones|
| 🎨 | **Living Canvas** — Shaders GLSL xerográficos que reaccionan a la música en tiempo real |
| ☁️ | **Serverless AWS** — Backend con API Gateway + Lambda + DynamoDB (pago por uso) |
| 🔄 | **CI/CD** — Deploy automático a develop y producción con GitHub Actions |

## 🏗️ Arquitectura

```
App Android (Kotlin + Compose + Media3)
        │
        ▼ HTTPS
API Gateway → Lambdareceptor → SQS → Lambdalyrics/mood → DeepSeek IA
                              │
                         DynamoDB (cache + estado)
```

## 🚀 Quick Start

```bash
# Clonar
git clone https://github.com/chrispam123/sonus-player.git
cd sonus-player

# Backend (compilar Lambdas)
cd backend
./gradlew :lambda-receptor:shadowJar :lambda-lyrics:shadowJar :lambda-mood:shadowJar

# Infra (requiere AWS CLI configurado)
cd ../infra
# PASO 0: crear bucket S3 manual (ver SPEC.md)
terraform init
terraform workspace select develop || terraform workspace new develop
terraform apply -var-file="environments/develop.tfvars" -var="deepseek_api_key=sk-xxx"

# App (Android Studio)
# Abrir raíz del proyecto → Run 'app'
```

## 📁 Estructura del proyecto

```
sonus-player/
├── app/                    # Android UI (Compose, shaders, navegación)
├── domain/                 # Casos de uso, interfaces, modelos
├── data/                   # Room, Retrofit, repositorios
├── backend/                # Lambdas Kotlin (JVM)
│   ├── lambda-receptor/    # Entrada HTTP → SQS
│   ├── lambda-lyrics/      # SQS → DeepSeek → DynamoDB
│   └── lambda-mood/        # SQS → DeepSeek → DynamoDB
├── infra/                  # Terraform (AWS IaaC)
├── .github/workflows/      # CI/CD (ci.yml + ci-prod.yml)
├── SPEC.md                 # Especificación completa del producto
└── README.md               # Este archivo
```

## 🔧 Stack

| Capa | Tecnología |
|------|-----------|
| **UI** | Jetpack Compose + Material 3 |
| **Arquitectura** | Clean Architecture + MVVM + Hilt DI |
| **Reproducción** | Media3 ExoPlayer |
| **Visualizador** | TextureView + EGL + GLSL (Munari 1967) |
| **BD Local** | Room (SQLite) |
| **Networking** | Retrofit + OkHttp |
| **Backend** | Kotlin JVM + AWS Lambda + API Gateway v2 |
| **Mensajería** | SQS + Dead Letter Queues |
| **BD Cloud** | DynamoDB PAY_PER_REQUEST |
| **IA** | DeepSeek API (chat + web search) |
| **Infra** | Terraform + S3 backend + Workspaces |
| **CI/CD** | GitHub Actions |

## 🌍 Entornos

| | Develop | Producción |
|---|---|---|
| **URL** | `sz4aqbavm2.execute-api.../develop` | `gsvb2col0d.execute-api.../prod` |
| **Rama** | `develop` | `main` |
| **Deploy** | Push automático | Merge develop→main |

## 📖 Documentación

La especificación completa del producto está en [`SPEC.md`](SPEC.md):
- Visión y propuesta de valor
- Arquitectura detallada
- Decisiones técnicas
- Histórico de bugs resueltos
- Roadmap

## 👤 Autor

**christian gohring** — [GitHub](https://github.com/chrispam123)

## 📄 Licencia

Este proyecto es privado. Todos los derechos reservados.
