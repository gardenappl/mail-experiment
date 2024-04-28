plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7
}

dependencies {
    // Jakarta Mail
    implementation("org.eclipse.angus:jakarta.mail:2.0.2")
    implementation("org.eclipse.angus:angus-activation:2.0.1")
    implementation("jakarta.activation:jakarta.activation-api:2.1.2")
}