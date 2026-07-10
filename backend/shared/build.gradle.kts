// ============================================================
// SHARED — Tipos compartidos entre Android y Lambdas
// ============================================================
// Este módulo contiene SOLO data classes y enums.
// No tiene dependencias de Android ni de AWS.
// Principio: Define el "contrato" de comunicación una sola vez.
// ============================================================

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Kotlinx Serialization — convierte data classes a JSON y viceversa
    // Se usa en Lambdas (para serializar respuestas HTTP)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}
