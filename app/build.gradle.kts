plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "ru.alexandrgert.gamespuzzle"
    compileSdk = 36
    defaultConfig {
        applicationId = "ru.alexandrgert.gamespuzzle"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        create("release") {
            val ks = rootProject.file("release.keystore")
            if (ks.exists()) {
                storeFile = ks
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                    ?.takeIf { it.isNotBlank() }
                    ?: throw GradleException("missing Android signing env")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                    ?.takeIf { it.isNotBlank() }
                    ?: throw GradleException("missing Android signing env")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
                    ?.takeIf { it.isNotBlank() }
                    ?: throw GradleException("missing Android signing env")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (rootProject.file("release.keystore").exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
