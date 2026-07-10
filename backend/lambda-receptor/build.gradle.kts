// ============================================================
// LAMBDA RECEPTOR — Build configuration
// ============================================================
// Esta Lambda es el punto de entrada HTTP desde API Gateway.
// Necesita:
//   - AWS SDK para DynamoDB (verificar caché) y SQS (encolar)
//   - Kotlinx Serialization para parsear JSON de API Gateway
//   - aws-lambda-java-core para el handler de Lambda
//
// El JAR que genera este módulo es lo que se sube a AWS Lambda.
// Usamos shadowJar para crear un "fat jar" con todas las
// dependencias incluidas (Lambda no tiene classpath externo).
// ============================================================

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    // Shadow plugin — genera un fat JAR con todas las dependencias
    // Sin esto, Lambda no encuentra las librerías en runtime
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Tipos compartidos con la app Android
    implementation(project(":shared"))

    // AWS Lambda handler — interfaz RequestHandler que AWS invoca
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")

    // AWS Lambda events — tipos para parsear eventos de API Gateway
    // APIGatewayProxyRequestEvent y APIGatewayProxyResponseEvent
    implementation("com.amazonaws:aws-lambda-java-events:3.11.4")

    // AWS SDK v2 para DynamoDB — verificar si la letra ya está cacheada
    implementation("software.amazon.awssdk:dynamodb:2.25.0")

    // AWS SDK v2 para SQS — encolar mensajes para procesamiento async
    implementation("software.amazon.awssdk:sqs:2.25.0")

    // Kotlinx Serialization — convertir data classes a/desde JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Kotlin coroutines para operaciones I/O no bloqueantes
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}

// Configuración del fat JAR para subir a Lambda
tasks.shadowJar {
    // El nombre del archivo JAR resultante
    archiveBaseName.set("lambda-receptor")
    archiveClassifier.set("")
    archiveVersion.set("")
}
