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
                val lwjglNatives = Pair(
                    System.getProperty("os.name")!!,
                    System.getProperty("os.arch")!!
                ).let { (name, arch) ->
                    when {
                        arrayOf("Linux", "FreeBSD", "SunOS", "Unit").any { name.startsWith(it) } ->
                            if (arrayOf("arm", "aarch64").any { arch.startsWith(it) })
                                "natives-linux${if (arch.contains("64") || arch.startsWith("armv8")) "-arm64" else "-arm32"}"
                            else
                                "natives-linux"
                        arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) } ->
                            "natives-macos${if (arch.startsWith("aarch64")) "-arm64" else ""}"
                        arrayOf("Windows").any { name.startsWith(it) } ->
                            if (arch.contains("64"))
                                "natives-windows${if (arch.startsWith("aarch64")) "-arm64" else ""}"
                            else
                                "natives-windows-x86"
                        else -> throw Error("Unrecognized or unsupported platform. Please set \"lwjglNatives\" manually")
                    }
                }
                implementation(libs.lwjgl)
                implementation("$lwjglModule:$lwjglNatives")

                implementation(libs.lwjgl.glfw)
                implementation("$lwjglGlfwModule:$lwjglNatives")

                implementation(libs.lwjgl.opengl)
                implementation("$lwjglOpenGlModule:$lwjglNatives")

                implementation(libs.lwjgl.openal)
                implementation("$lwjglOpenAlModule:$lwjglNatives")

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
