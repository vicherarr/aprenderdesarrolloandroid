plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.aprender.wear"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aprender.holaandroid"   // mismo que el móvil (Guía 61)
        minSdk = 30                                    // Wear OS 3+
        targetSdk = 36
        versionCode = 1001                             // > que el del móvil (Guía 61)
        versionName = "1.0"
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
    kotlinOptions { jvmTarget = "11" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)

    // Compose for Wear OS (Guías 54–55)
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.navigation)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.wear.tooling.preview)
    implementation(libs.wear.compose.ui.tooling)
    implementation(libs.wear)                       // AmbientLifecycleObserver (Guía 59)
    implementation(libs.wear.ongoing)               // Ongoing Activity (Guía 59)

    // Tiles y Complications (Guía 56)
    implementation(libs.wear.tiles)
    implementation(libs.wear.tiles.material)
    implementation(libs.wear.protolayout)
    implementation(libs.wear.protolayout.material3)
    implementation(libs.wear.complications.data.source.ktx)

    // Health Services (Guía 57)
    implementation(libs.health.services.client)
    implementation(libs.kotlinx.coroutines.guava)   // await() sobre ListenableFuture

    // Data Layer (Guía 58)
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services) // await() sobre Task

    debugImplementation(libs.androidx.ui.tooling)
}
