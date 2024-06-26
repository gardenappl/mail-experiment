plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.serialization") version Build.Versions.kotlin
}

android {
    namespace = "garden.appl.mail"
    compileSdk = 34

    defaultConfig {
        applicationId = "garden.appl.mail"
        minSdk = Build.Android.minSdk
        targetSdk = Build.Android.targetSdk
        versionCode = Build.Android.versionCode
        versionName = Build.Android.versionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        named("release").configure {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
    buildFeatures {
        viewBinding = true
    }
    packaging {
        // Resolve conflict between Jakarta Mail and Jakarta Activation
        // see https://eclipse-ee4j.github.io/angus-mail/Android
        resources.pickFirsts += "META-INF/LICENSE.md"
        resources.pickFirsts += "META-INF/NOTICE.md"
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")
    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation("androidx.room:room-runtime:2.5.1")
    implementation("androidx.room:room-ktx:2.5.1")
    implementation("androidx.core:core-ktx:+")
    kapt("androidx.room:room-compiler:2.5.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Jakarta Mail
    implementation("org.eclipse.angus:jakarta.mail:2.0.2")
    implementation("org.eclipse.angus:angus-activation:2.0.1")
    implementation("jakarta.activation:jakarta.activation-api:2.1.2")
    implementation("org.pgpainless:pgpainless-core:1.6.1")
    implementation(project(":jmap-jakarta"))
}