import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("littlekt.convention.publication")
    id("org.jetbrains.dokka")
}

repositories {
    mavenCentral()
}

kotlin {
    android {
        publishLibraryVariants("release", "debug")
    }
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
                implementation("$lwjglModule:natives-windows-arm64")
                implementation("$lwjglModule:natives-linux")
                implementation("$lwjglModule:natives-linux-arm64")
                implementation("$lwjglModule:natives-macos")
                implementation("$lwjglModule:natives-macos-arm64")

                implementation(libs.lwjgl.glfw)
                implementation("$lwjglGlfwModule:natives-windows")
                implementation("$lwjglGlfwModule:natives-windows-arm64")
                implementation("$lwjglGlfwModule:natives-linux")
                implementation("$lwjglGlfwModule:natives-linux-arm64")
                implementation("$lwjglGlfwModule:natives-macos")
                implementation("$lwjglGlfwModule:natives-macos-arm64")


                implementation(libs.lwjgl.opengl)
                implementation("$lwjglOpenGlModule:natives-windows")
                implementation("$lwjglOpenGlModule:natives-windows-arm64")
                implementation("$lwjglOpenGlModule:natives-linux")
                implementation("$lwjglOpenGlModule:natives-linux-arm64")
                implementation("$lwjglOpenGlModule:natives-macos")
                implementation("$lwjglOpenGlModule:natives-macos-arm64")

                implementation(libs.lwjgl.openal)
                implementation("$lwjglOpenAlModule:natives-windows")
                implementation("$lwjglOpenAlModule:natives-windows-arm64")
                implementation("$lwjglOpenAlModule:natives-linux")
                implementation("$lwjglOpenAlModule:natives-linux-arm64")
                implementation("$lwjglOpenAlModule:natives-macos")
                implementation("$lwjglOpenAlModule:natives-macos-arm64")

                implementation(libs.mp3.decoder)
            }
        }
        val jvmTest by getting
        val jsMain by getting
        val jsTest by getting
        val androidMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.android)
            }
        }
        val androidTest by getting

        val jvmAndroidMain = maybeCreate("jvmAndroidMain")

        jvmAndroidMain.dependsOn(commonMain)
        androidMain.dependsOn(jvmAndroidMain)
        jvmMain.dependsOn(jvmAndroidMain)
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
