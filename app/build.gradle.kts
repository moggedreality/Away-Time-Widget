import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    // Code quality - temporarily disabled for build
    // alias(libs.plugins.ktlint)
    // alias(libs.plugins.detekt)
    // alias(libs.plugins.spotless)
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

// Kotlin 2.0+ compilerOptions DSL
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.valueOf(libs.versions.jvmTarget.get()))
    }
}