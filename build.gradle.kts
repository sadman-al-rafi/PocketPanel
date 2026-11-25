// Temporary M0b toolchain-verification scaffold.
//
// AGP 9.5.2 declares a runtime dependency on KGP 2.2.10. Kotlin 2.4.21
// is the version recorded in docs/DECISIONS.md D005, and it is applied
// through the documented "upgrade to a higher KGP version" mechanism so
// that the Kotlin compiler and the Compose compiler plugin are the same
// version. This is deliberate, not a compatibility workaround.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
}
