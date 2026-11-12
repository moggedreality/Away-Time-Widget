// Top-level build file for Away Time
// Versions are centralized in gradle/libs.versions.toml
// Update versions there, then run: ./gradlew dependencyUpdates

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
    // Configure JVM target for all Kotlin compile tasks
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = libs.versions.jvmTarget.get()
        }
    }
}