plugins {
    id("java")
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()

    sourceSets {
        val templates by creating {
            compileClasspath += sourceSets["main"].output
            runtimeClasspath += sourceSets["main"].output
        }

        main { runtimeClasspath += templates.output }
    }
}
