import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

// Version values live in a tracked file so they are reviewed in one place.
val releaseVersion = Properties().apply {
    rootProject.file("release/version.properties").inputStream().use { load(it) }
}
val versionCodeValue = releaseVersion.getProperty("versionCode")?.toIntOrNull()
    ?: error("release/version.properties must define a numeric versionCode")
val versionNameValue = releaseVersion.getProperty("versionName")?.takeIf { it.isNotBlank() }
    ?: error("release/version.properties must define versionName")

// Signing never lives in tracked files. Environment variables take priority
// over a local, untracked keystore.properties. Missing values leave the
// release build unable to sign, but never affect debug builds.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}
fun releaseSetting(envSuffix: String, propertyName: String): String? =
    System.getenv("POCKETPANEL_RELEASE_$envSuffix")
        ?: keystoreProperties.getProperty(propertyName)?.takeIf { it.isNotBlank() }

val releaseStoreFile = releaseSetting("STORE_FILE", "storeFile")
val releaseStorePassword = releaseSetting("STORE_PASSWORD", "storePassword")
val releaseKeyAlias = releaseSetting("KEY_ALIAS", "keyAlias")
val releaseKeyPassword = releaseSetting("KEY_PASSWORD", "keyPassword")

android {
    namespace = "dev.pocketpanel"

    // Compose 1.13.0 (BOM 2026.11.01) AAR metadata requires API 37.1+.
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        // Immutable release identity: do not change this for updates.
        applicationId = "dev.pocketpanel"
        minSdk = 26
        targetSdk = 36
        versionCode = versionCodeValue
        versionName = versionNameValue
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // A non-existent placeholder keeps configuration valid when no
            // release credentials are supplied; only release tasks fail.
            storeFile = rootProject.file(
                releaseStoreFile ?: "release-keystore-not-configured.jks",
            )
            storePassword = releaseStorePassword.orEmpty()
            keyAlias = releaseKeyAlias.orEmpty()
            keyPassword = releaseKeyPassword.orEmpty()
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        // The WireGuard tunnel library uses java.time and List.of. The
        // desugared library supplies them below the platform API level.
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.savedstate)
    implementation(libs.androidx.datastore.preferences)

    // Official WireGuard tunnel library. It declares the VpnService and
    // packages the native backend for every supported ABI.
    implementation(libs.wireguard.tunnel)

    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.compose.ui.test.junit4)
}
