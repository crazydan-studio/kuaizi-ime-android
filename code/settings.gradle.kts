rootProject.name = "Kuaizi-IME"

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven { url = uri("https://jitpack.io") }
    }
}

include(":app")
include(":engine")
include(":ui")
include(":app-codegen")
