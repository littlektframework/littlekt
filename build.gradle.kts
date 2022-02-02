val littleKtVersion: String by project

allprojects {
    group = "com.lehaine.littlekt"
    version = littleKtVersion
    extra["isReleaseVersion"] = !version.toString().endsWith("SNAPSHOT")
}

plugins {
    kotlin("multiplatform") version "1.6.0" apply false
}