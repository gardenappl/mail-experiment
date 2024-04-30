// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    kotlin("plugin.serialization") version Build.Versions.kotlin apply false
}

buildscript {
    repositories {
        mavenCentral()
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.1")
        classpath(kotlin("gradle-plugin", version = Build.Versions.kotlin))

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}