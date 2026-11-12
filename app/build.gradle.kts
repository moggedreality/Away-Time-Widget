import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    // Code quality
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless)
}

android {
    namespace = "com.stxrs.awaytime"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.stxrs.awaytime"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 4
        versionName = "2.2.0"

        // Test instrumentation runner
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Signing configs - release signing via environment variables (CI)
    // Create key.properties locally for local release builds (never commit!)
    signingConfigs {
        create("release") {
            // Configured via key.properties or CI secrets
            // See: https://developer.android.com/studio/publish/app-signing
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false // Enable R8/ProGuard when ready: true
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Signing configured via key.properties or CI environment variables
            // signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = libs.versions.jvmTarget.get()
    }

    // Lint configuration
    lint {
        abortOnError = false
        checkReleaseBuilds = true
        // Enable additional checks
        enable("UnusedResources")
        enable("MissingTranslation")
        disable("TypographyFractions")
        disable("TypographyQuotes")
    }

    // Packaging options
    packaging {
        resources {
            excludes += "/META-INF/*.kotlin_module"
        }
    }
}

dependencies {
    // AndroidX - uncomment as needed
    // implementation(libs.androidx.core.ktx)
    // implementation(libs.androidx.lifecycle.runtime.ktx)
    // implementation(libs.androidx.lifecycle.viewmodel.ktx)
    // implementation(libs.androidx.activity.ktx)

    // Coroutines
    // implementation(libs.kotlinx.coroutines.android)

    // Serialization
    // implementation(libs.kotlinx.serialization.json)

    // Room
    // implementation(libs.room.runtime)
    // implementation(libs.room.ktx)
    // ksp(libs.room.compiler)

    // DataStore
    // implementation(libs.datastore.preferences)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)

    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.espresso.contrib)
}

// ============================================================================
// CODE QUALITY CONFIGURATION
// ============================================================================

// KTLINT - Kotlin formatting (official style guide)
ktlint {
    version = libs.versions.ktlint.get()
    // Enable Android-specific rules
    android = true
    // Output formats
    reporters {
        reporter("plain")
        reporter("checkstyle", file("$buildDir/reports/ktlint/ktlint-checkstyle.xml"))
    }
}

// DETEKT - Static analysis
detekt {
    toolVersion = libs.versions.detekt.get()
    config = files("$projectDir/config/detekt/detekt.yml")
    buildUponDefaultConfig = true
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(false)
        sarif.required.set(false)
    }
}

// SPOTLESS - Format enforcement + license headers
spotless {
    kotlin {
        // Use ktlint for formatting
        ktlint().version(libs.versions.ktlint.get())
    }

    // License header for all Kotlin files
    licenseHeader(
        """
            MIT License

            Copyright (c) 2024 Away Time Contributors

            Permission is hereby granted, free of charge, to any person obtaining a copy
            of this software and associated documentation files (the "Software"), to deal
            in the Software without restriction, including without limitation the rights
            to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
            copies of the Software, and to permit persons to whom the Software is
            furnished to do so, subject to the following conditions:

            The above copyright notice and this permission notice shall be included in all
            copies or substantial portions of the Software.

            THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
            IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
            FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
            AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
            LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
            OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
            SOFTWARE.
        """.trimIndent()
    )

    // Apply to all Kotlin files
    target("**/*.kt")
}

// Tasks for CI
tasks {
    // Check task runs all quality checks
    check {
        dependsOn("ktlintCheck", "detekt", "spotlessCheck")
    }

    // Format task auto-formats code
    register("format", SpotlessApply::class) {
        group = "verification"
        description = "Auto-format code with Spotless"
    }
}