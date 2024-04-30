plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    kotlin("plugin.serialization") version "1.9.23"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Jakarta Mail
    implementation("org.eclipse.angus:jakarta.mail:2.0.2")
    implementation("org.eclipse.angus:angus-activation:2.0.1")
    implementation("jakarta.activation:jakarta.activation-api:2.1.2")

    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.12")
}