allprojects {
    group = "com.lehaine.littlekt"
    version = "0.0.1-SNAPSHOT"
    extra["isReleaseVersion"] = !version.toString().endsWith("SNAPSHOT")
}

plugins {
    kotlin("multiplatform") version "1.6.0" apply false
}