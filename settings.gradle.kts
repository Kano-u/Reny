pluginManagement {
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
        // LSPosed 模块所需的 Xposed API
        maven { url = uri("https://api.xposed.info/") }
    }
}

rootProject.name = "Reny"
include(":app")
