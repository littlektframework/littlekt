val littleKtVersion: String by project

buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.bundles.plugins)
    }
}


allprojects {
    repositories {
        google()
        mavenCentral()
    }

    group = "com.lehaine.littlekt"
    version = littleKtVersion
    extra["isReleaseVersion"] = !version.toString().endsWith("SNAPSHOT")
}