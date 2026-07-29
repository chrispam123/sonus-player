// ============================================================
// LAMBDA LYRICS — Build configuration
// ============================================================
// Esta Lambda se dispara automáticamente cuando SQS lyrics_queue
// tiene mensajes pendientes. Llama a DeepSeek para generar la
// letra y la guarda en DynamoDB.
// ============================================================

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared"))

    // Handler de Lambda para eventos SQS
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("com.amazonaws:aws-lambda-java-events:3.11.4")

    // DynamoDB — guardar letra cacheada + actualizar estado
    implementation("software.amazon.awssdk:dynamodb:2.25.0")

    // HTTP client para llamar a DeepSeek API
    // OkHttp es ligero y compatible con Kotlin
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}

tasks.shadowJar {
    archiveBaseName.set("lambda-lyrics")
    archiveClassifier.set("")
    archiveVersion.set("")
}
