// Top-level build file for Away Time
// Versions are centralized in gradle/libs.versions.toml
// Update versions there, then run: ./gradlew dependencyUpdates

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false

    // Code quality plugins (apply to subprojects via convention)
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.spotless) apply false

    // Version catalog update helper
    alias(libs.plugins.version.catalog.update) apply false
}

// Define common configuration for all subprojects
allprojects {
    group = "com.stxrs.awaytime"
    version = "2.2.0"
}

tasks {
    // Configure JVM target for all Kotlin compile tasks (Kotlin 2.0+ compilerOptions DSL)
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.valueOf(libs.versions.jvmTarget.get()))
        }
    }
}