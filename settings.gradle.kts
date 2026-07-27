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
        // Pangle SDK(com.pangle.global:pag-sdk)는 ByteDance 자체 maven 에서만 배포된다
        maven { url = uri("https://artifact.bytedance.com/repository/pangle") }
    }
}

rootProject.name = "FearIndex-Android"

include(":app")
include(":core")
include(":domain")
include(":data")
include(":presentation")
