import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    id("module.publication")
}

repositories { mavenCentral() }

kotlin {
    jvm {
        compilerOptions { jvmTarget = JvmTarget.JVM_22 }
        testRuns["test"].executionTask.configure { useJUnit() }
    }
    js(KotlinJsCompilerType.IR) {
        browser {
            binaries.executable()
            testTask { useKarma { useChromeHeadless() } }
        }

        this.attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)

        compilations.all { kotlinOptions.sourceMap = true }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
                implementation(project(":core"))
            }
        }
        val commonTest by getting { dependencies { implementation(kotlin("test")) } }
    }

    task("testClasses")
    task("compileJava")
}
