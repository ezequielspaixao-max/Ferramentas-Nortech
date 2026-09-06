plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "br.com.nortech.capacitores"
    compileSdk = 36

    defaultConfig {
        applicationId = "br.com.nortech.capacitores"
        minSdk = 24
        targetSdk = 36
        versionCode = 13
        versionName = "1.2"
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.compose.ui:ui:1.8.2")
    implementation("androidx.compose.ui:ui-tooling-preview:1.8.2")
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    debugImplementation("androidx.compose.ui:ui-tooling:1.8.2")
}
