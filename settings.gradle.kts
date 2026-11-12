pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    // Enable version catalogs
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.android.application") {
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
    // Version catalog auto-loaded from gradle/libs.versions.toml (Gradle 8.14+)
}

rootProject.name = "AwayTime"

include(":app")

