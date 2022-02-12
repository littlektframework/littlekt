import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("littlekt.convention.publication")
}

repositories {
    mavenCentral()
}

kotlin {
    android()
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }
    js(KotlinJsCompilerType.IR) {
        browser {
            binaries.executable()
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }

        this.attributes.attribute(
            KotlinPlatformType.attribute,
            KotlinPlatformType.js
        )

        compilations.all {
            kotlinOptions.sourceMap = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.atomicfu)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                api(project(":tools"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                val lwjglModule = "${libs.lwjgl.asProvider().get().module}:${
                    libs.lwjgl.asProvider().get().versionConstraint.requiredVersion
                }"
                val lwjglGlfwModule =
                    "${libs.lwjgl.glfw.get().module}:${libs.lwjgl.glfw.get().versionConstraint.requiredVersion}"
                val lwjglOpenGlModule = "${libs.lwjgl.opengl.get().module}:${
                    libs.lwjgl.opengl.get().versionConstraint.requiredVersion
                }"
                val lwjglOpenAlModule = "${libs.lwjgl.openal.get().module}:${
                    libs.lwjgl.openal.get().versionConstraint.requiredVersion
                }"
                implementation(libs.lwjgl)
                implementation("$lwjglModule:natives-windows")
                implementation("$lwjglModule:natives-linux")
                implementation("$lwjglModule:natives-macos")

                implementation(libs.lwjgl.glfw)
                implementation("$lwjglGlfwModule:natives-windows")
                implementation("$lwjglGlfwModule:natives-linux")
                implementation("$lwjglGlfwModule:natives-macos")


                implementation(libs.lwjgl.opengl)
                implementation("$lwjglOpenGlModule:natives-windows")
                implementation("$lwjglOpenGlModule:natives-linux")
                implementation("$lwjglOpenGlModule:natives-macos")

                implementation(libs.lwjgl.openal)
                implementation("$lwjglOpenAlModule:natives-windows")
                implementation("$lwjglOpenAlModule:natives-windows-x86")
                implementation("$lwjglOpenAlModule:natives-linux")
                implementation("$lwjglOpenAlModule:natives-linux-arm32")
                implementation("$lwjglOpenAlModule:natives-linux-arm64")
                implementation("$lwjglOpenAlModule:natives-macos")

                implementation(libs.mp3.decoder)
            }
        }
        val jvmTest by getting
        val jsMain by getting
        val jsTest by getting
        val androidMain by getting
        val androidTest by getting

        androidMain.dependsOn(commonMain)
        jvmMain.dependsOn(commonMain)
        jsMain.dependsOn(commonMain)
        androidTest.dependsOn(commonTest)
        jvmTest.dependsOn(commonTest)
        jsTest.dependsOn(commonTest)

        all {
            languageSettings.apply {
                progressiveMode = true
                optIn("kotlin.contracts.ExperimentalContracts")
                optIn("kotlin.time.ExperimentalTime")
            }
        }
    }
}

android {
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    compileSdk = (findProperty("android.compileSdk") as String).toInt()

    defaultConfig {
        minSdk = (findProperty("android.minSdk") as String).toInt()
        targetSdk = (findProperty("android.targetSdk") as String).toInt()
    }
}
