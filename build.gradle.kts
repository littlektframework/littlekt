import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

plugins {
    alias(libs.plugins.download).apply(false)
    alias(libs.plugins.kotlin.multiplatform).apply(false)
    alias(libs.plugins.kotlin.serialization).apply(false)
    alias(libs.plugins.kotlin.jvm).apply(false)
}

val littleKtVersion: String by project

allprojects {
    group = "com.littlekt"
    version = littleKtVersion
    extra["isReleaseVersion"] = !littleKtVersion.endsWith("SNAPSHOT")
}

plugins.withType<YarnPlugin> {
    the<YarnRootExtension>().apply { yarnLockMismatchReport = YarnLockMismatchReport.WARNING }
}

// TODO: remove later
configurations.all {
    resolutionStrategy {
        cacheChangingModulesFor(0, "seconds")
        cacheDynamicVersionsFor(0, "minutes")
    }
}