plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.yakuphanuslu.kombin"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.yakuphanuslu.kombin"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    // API bağlantısı (Retrofit)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

// Resim yükleme (Coil - Modern ve Kotlin dostudur)
    implementation("io.coil-kt:coil:2.4.0")

// Gemini AI (Google AI Edge SDK)
    implementation("com.google.ai.client.generativeai:generativeai:0.4.0")
// Başına "com." ekledik
    implementation("com.github.bumptech.glide:glide:4.16.0")

    implementation("com.google.android.material:material:1.9.0") // Versiyon güncel olabilir

}