plugins {
    id("java-library")
    id("module.publication")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()

    sourceSets {
        main {
            java.srcDirs(
                "src/main/java",
                "src/main/androidboot",
                "src/main/android",
                "src/generated/java"
            )
        }
    }
}

dependencies { implementation(libs.javax.annotations) }
