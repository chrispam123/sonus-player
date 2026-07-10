// ============================================================
// SONUS BACKEND — Build raíz
// ============================================================
// Configuración compartida para todos los módulos del backend.
// Usamos Kotlin JVM (no Android) para correr en AWS Lambda.
// ============================================================

plugins {
    // Kotlin JVM — corre en la JVM de AWS Lambda
    kotlin("jvm") version "2.0.21" apply false
    // Serialización — para convertir data classes a/desde JSON
    kotlin("plugin.serialization") version "2.0.21" apply false
}

// Configuración común para todos los submódulos
subprojects {
    repositories {
        mavenCentral()
    }
}
