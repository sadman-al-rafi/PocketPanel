// Temporary M0b toolchain-verification module.
// This is not the PocketPanel application: it contains no features and
// exists only to prove that the pinned toolchain resolves, compiles
// Kotlin with the Compose compiler, and packages a debug APK.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "dev.pocketpanel"

    // Compose 1.13.0 (BOM 2026.11.01) AAR metadata requires API 37.1+.
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "dev.pocketpanel"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.0-verify"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
}
