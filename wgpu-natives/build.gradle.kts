import de.undercouch.gradle.tasks.download.Download

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.download)
    id("module.publication")
}

tasks {
    register("downloadWgpuNativeHeaders", Download::class) {
        val headersDirectory = file("$projectDir/src/main/c")
        onlyIf("wgpu headers are empty") {
            return@onlyIf if (headersDirectory.exists() && headersDirectory.isDirectory) {
                val headerFiles =
                    headersDirectory.walkTopDown().filter { it.extension == "h" }.toList()
                headerFiles.isEmpty()
            } else {
                true
            }
        }
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
        val headersDir = "$buildDir/wgpu_headers"
        val headersDirFile = file(headersDir)
        onlyIf("WGPU headers are downloaded") {
            headersDirFile.exists() && headersDirFile.walkTopDown().any { it.extension == "h" }
        }

        from(headersDir) { include("**.h") }
        into("$projectDir/src/main/c")
        delete(headersDir)
    }

    register("downloadWgpuNativeBinaries", Download::class) {
        val wgpuNativeVersion: String by rootProject.extra
        val baseUrl = "https://github.com/gfx-rs/wgpu-native/releases/download/v$wgpuNativeVersion"
        val nativesResourcesDir = file("$projectDir/src/main/resources")
        val downloadDir =
            file("${layout.buildDirectory.asFile.get().path}/wgpu_binaries").apply { mkdirs() }
        onlyIf("binaries are empty") {
            if (nativesResourcesDir.exists() && nativesResourcesDir.isDirectory) {
                val nativeFiles =
                    nativesResourcesDir.walkTopDown().filter { it.extension == "dll" }.toList()
                nativeFiles.isEmpty()
            } else {
                true
            }
        }
        src(
            listOf(
                "$baseUrl/wgpu-windows-x86_64-msvc-release.zip",
                "$baseUrl/wgpu-macos-x86_64-release.zip ",
                "$baseUrl/wgpu-macos-aarch64-release.zip ",
                "$baseUrl/wgpu-linux-x86_64-release.zip",
            )
        )
        dest(downloadDir)
    }

    register("installWgpuNativeBinaries", Copy::class) {
        dependsOn("downloadWgpuNativeBinaries")
        val buildDir = layout.buildDirectory.asFile.get().path
        val binariesDir = "$buildDir/wgpu_binaries"
        val binariesDirFile = file(binariesDir)
        onlyIf("binaries are downloaded") {
            binariesDirFile.exists() && binariesDirFile.walkTopDown().any { it.extension == "zip" }
        }

        from(zipTree("$binariesDir/wgpu-windows-x86_64-msvc-release.zip")) { include("**.dll") }
        from(zipTree("$binariesDir/wgpu-macos-x86_64-release.zip")) { include("**.dylib") }
        from(zipTree("$binariesDir/wgpu-macos-aarch64-release.zip")) {
            include("**.dylib")
            rename { it.replace(".dylib", "_aarch64.dylib") }
        }
        from(zipTree("$binariesDir/wgpu-linux-x86_64-release.zip")) { include("**.so") }

        into("$projectDir/src/main/resources")
        delete(binariesDir)
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
