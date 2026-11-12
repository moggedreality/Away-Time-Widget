pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    // Enable version catalogs
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.android.application" || requested.id.id == "org.jetbrains.kotlin.android") {
                useVersion(providers.gradleProperty("agp.version").getOrNull() ?: "8.13.2")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }

    // Enable version catalog for all projects
    versionCatalogs {
        create("libs") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "AwayTime"

include(":app")

// Enable configuration cache (Gradle 8.14+)
configure<org.gradle.api.initialization.Settings> {
    isConfigurationCache = true
}