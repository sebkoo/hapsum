pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "hapsum"
include(":app")
include(":core:ai")
include(":core:model")
include(":core:testing")
include(":core:data")
include(":core:designsystem")
include(":core:mvi")
include(":feature:ledger")
include(":feature:capture")
include(":feature:confirm")
include(":feature:insights")
