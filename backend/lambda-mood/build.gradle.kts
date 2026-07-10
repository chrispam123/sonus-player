// ============================================================
// LAMBDA MOOD — Build configuration
// ============================================================
// Esta Lambda analiza el historial de escucha horario y
// determina el estado de ánimo del usuario usando DeepSeek.
// Devuelve: mood label + shader visual + links de YouTube.
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
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("com.amazonaws:aws-lambda-java-events:3.11.4")
    implementation("software.amazon.awssdk:dynamodb:2.25.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}

tasks.shadowJar {
    archiveBaseName.set("lambda-mood")
    archiveClassifier.set("")
    archiveVersion.set("")
}
