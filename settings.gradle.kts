pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "VigiPro"

include(":app")

// Core modules
include(":core:core-ui")
include(":core:core-network")
include(":core:core-data")
include(":core:core-model")

// Feature modules
include(":feature:feature-auth")
include(":feature:feature-dashboard")
include(":feature:feature-player")
include(":feature:feature-devices")
include(":feature:feature-access-control")
include(":feature:feature-settings")
