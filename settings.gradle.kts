pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

includeBuild("gradle-plugins/convention-plugins")
rootProject.name = "littlekt"
include("core")
include("extensions:tools")
include("extensions:gradle:texturepacker")
include("samples")