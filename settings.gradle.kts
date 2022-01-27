pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

includeBuild("gradle-plugins/convention-plugins")
rootProject.name = "littlekt"
include("core")
include("gradle-plugins:extensions:texturepacker")
include("samples")
