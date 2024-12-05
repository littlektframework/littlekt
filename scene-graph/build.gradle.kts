import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("module.publication")
}

kotlin {
    tasks.withType<JavaExec> { jvmArgs("--enable-native-access=ALL-UNNAMED") }
    jvm { compilerOptions { jvmTarget = JvmTarget.JVM_22 } }
    js(KotlinJsCompilerType.IR) {
        browser {
            binaries.executable()
            testTask { useKarma { useChromeHeadless() } }
        }

        this.attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)

        compilerOptions { sourceMap = true }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(project(":core"))
            }
        }
    }
}
