import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("module.publication")
}

kotlin {
    tasks.withType<JavaExec> { jvmArgs("--enable-preview", "--enable-native-access=ALL-UNNAMED") }
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
            compileJavaTaskProvider?.get()?.options?.compilerArgs?.add("--enable-preview")
        }
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
                implementation(libs.kotlinx.coroutines.core)
                implementation(project(":core"))
            }
        }
    }
}
