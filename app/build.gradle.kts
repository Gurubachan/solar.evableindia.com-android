plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt") // Add this line
    id("kotlin-parcelize")
    alias(libs.plugins.google.gms.google.services) // Changed to use alias
}

kotlin {
    jvmToolchain(17) // Or your desired JDK version (e.g., 8, 11)
}
android {
    namespace = "com.solar.ev"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.solar.ev"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "2.2.3"

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { // Added this block
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.material.v1120)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.mediation.test.suite)
// Or the latest version
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Retrofit for networking
    implementation(libs.retrofit) // Or the latest version
    implementation(libs.converter.gson) // For JSON parsing (or Moshi, Jackson)
    implementation(libs.logging.interceptor) // Optional: For logging network requests

    // Kotlin Coroutines for asynchronous operations
    implementation(libs.kotlinx.coroutines.core) // Or the latest version
    implementation(libs.kotlinx.coroutines.android)

    // ViewModel and LiveData (recommended for managing UI-related data)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.activity.ktx) // For by viewModels()

    implementation(libs.glide) // Latest stable version
    kapt(libs.compiler)

    implementation(libs.play.services.location) // Or the latest version

}
