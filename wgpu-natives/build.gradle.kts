import de.undercouch.gradle.tasks.download.Download

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.download)
    id("module.publication")
}

tasks {
    register("downloadWgpuNativeHeaders", Download::class) {
        val webgpuHeadersSHA: String by rootProject.extra
        val wgpuNativeVersion: String by rootProject.extra
        val webgpuHeaderUrl =
            "https://raw.githubusercontent.com/webgpu-native/webgpu-headers/$webgpuHeadersSHA/webgpu.h"
        val wgpuHeaderUrl =
            "https://raw.githubusercontent.com/gfx-rs/wgpu-native/v$wgpuNativeVersion/ffi/wgpu.h"

        onlyIfModified(true)
        src(listOf(webgpuHeaderUrl, wgpuHeaderUrl))
        dest("${layout.buildDirectory.asFile.get().path}/wgpu_headers")
    }

    register("installWgpuNativeHeaders", Copy::class) {
        dependsOn("downloadWgpuNativeHeaders")
        val buildDir = layout.buildDirectory.asFile.get().path

        from("$buildDir/wgpu_headers") { include("**.h") }
        into("$projectDir/src/main/c")
    }

    register("downloadWgpuNativeBinaries", Download::class) {
        val wgpuNativeVersion: String by rootProject.extra
        val baseUrl = "https://github.com/gfx-rs/wgpu-native/releases/download/v$wgpuNativeVersion"
        onlyIfModified(true)
        src(
            listOf(
                "$baseUrl/wgpu-windows-x86_64-msvc-release.zip",
                "$baseUrl/wgpu-macos-x86_64-release.zip ",
                "$baseUrl/wgpu-macos-aarch64-release.zip ",
                "$baseUrl/wgpu-linux-x86_64-release.zip",
            )
        )
        dest(layout.buildDirectory.asFile.get())
    }

    register("installWgpuNativeBinaries", Copy::class) {
        dependsOn("downloadWgpuNativeBinaries")
        val buildDir = layout.buildDirectory.asFile.get().path

        from(zipTree("$buildDir/wgpu-windows-x86_64-msvc-release.zip")) { include("**.dll") }
        from(zipTree("$buildDir/wgpu-macos-x86_64-release.zip")) { include("**.dylib") }
        from(zipTree("$buildDir/wgpu-macos-aarch64-release.zip")) {
            include("**.dylib")
            rename { it.replace(".dylib", "_aarch64.dylib") }
        }
        from(zipTree("$buildDir/wgpu-linux-x86_64-release.zip")) { include("**.so") }

        into("$projectDir/src/main/resources")
    }

    register("downloadAndInstallWgpuNativeBinariesAndHeaders") {
        dependsOn("installWgpuNativeHeaders")
        dependsOn("installWgpuNativeBinaries")
    }

    val compileJava by getting { dependsOn("downloadAndInstallWgpuNativeBinariesAndHeaders") }

    val processResources by getting { dependsOn("downloadAndInstallWgpuNativeBinariesAndHeaders") }
}

kotlin {
    dependencies { testImplementation(libs.kotlin.test) }

    tasks.test { useJUnitPlatform() }
}

publishing {
    publications { create<MavenPublication>("wgpu-natives") { from(components["java"]) } }
}
