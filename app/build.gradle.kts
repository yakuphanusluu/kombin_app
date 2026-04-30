import java.util.Properties
import java.io.FileInputStream

// 1. Plugins bloğu SADECE BİR KEZ ve en üstte olmalı.
// Alias veya kotlin("android") gibi çiftlemelerden kaçınıyoruz.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// 2. Değişken tanımlamaları
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}
val geminiApiKey: String = localProperties.getProperty("GEMINI_API_KEY") ?: ""

android {
    namespace = "com.yakuphanuslu.kombin"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yakuphanuslu.kombin"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // BuildConfig üzerinden API Key aktarımı
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // Android Temel
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Retrofit ve Görsel Kütüphaneleri
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Gemini AI SDK
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
}