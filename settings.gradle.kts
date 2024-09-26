pluginManagement {
    includeBuild("convention-plugins")
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0" }

dependencyResolutionManagement {
    repositories {
        //mavenLocal()
        google()
        mavenCentral()
        //wgpu4k snapshot & preview repository
        maven("https://gitlab.com/api/v4/projects/25805863/packages/maven")
        maven(url = "https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
    }
}

rootProject.name = "littlekt"

include(":core")

include("wgpu-natives")

include("wgpu-ffm")

include("examples")

include("scene-graph")

include("extensions:tools")

include("extensions:gradle:texturepacker")
