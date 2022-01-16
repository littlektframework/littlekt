# LittleKt - A 2D game framework written in Kotlin
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/littlektframework/littlekt/blob/master/LICENSE)
[![build](https://github.com/littlektframework/littlekt/actions/workflows/build.yml/badge.svg?branch=master)](https://github.com/littlektframework/littlekt/actions/workflows/build.yml)

**Currently in development.**

**[Features](https://littlekt.com/features/)** - **[Wiki](https://littlekt.com/wiki/)** - **[Samples](https://github.com/littlektframework/littlekt-samples)**

**LittleKt is a Kotlin multiplatform 2D game development framework based on OpenGL** that is inspired by libGDX and KorGE. The goal of this project is to allow the freedom and flexibility that libGDX offers with enjoyable idiomatic features coded in Kotlin that KorGE has to offer.

Check out some planned [features](https://github.com/littlektframework/littlekt/labels/enhancement)

### Install

If you are eager to try LittleKt you can give the latest SNAPSHOT build a try until an actual release is made:

**build.gradle.kts**:
```kotlin
repositories {
    maven(url ="https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11" // littlekt targets jvm 11 so we must target atleast 11
        }
    }
}

val littleKtVersion = "0.0.1-SNAPSHOT"
val kotlinCoroutinesVersion = "1.6.0-RC" // or whatever version you are using

sourceSets {
    val commonMain by getting {
        dependencies {
            implementation("com.lehaine.littlekt:core:$littleKtVersion")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")  // littlekt requires coroutines library on the classpath
        }
    }
}
```

### Current targets

| Platform | Implemented |
| -------- | :---------: |
| Desktop (JVM) | ✅ |
| Web (WebGL/2) | ✅ |
| Android | Not started |
| iOS | Not started |
| Desktop (Native) | Not started |

### Acknowledgements
LittleKt was put together based on bits and pieces of features found across multiple engines/frameworks and languages that were very enjoyable to use and flexible. If a piece a code looks familiar, feel free to open an issue with details, so that we can properly attribute the code.

A big thanks to the folks over on libGDX and KTX, KorGE, and MiniGDX.

The very popular and amazing [libGDX](https://github.com/libgdx/libgdx) which is the main inspiration of this framework as well as the Kotlin framework [KTX](https://github.com/libktx/ktx) for the clever and awesome utilites and extensions built on top of libGDX.

Carlos Velasco's (soywiz) awesome Kotlin game engine [KorGE](https://github.com/korlibs/korge) which has a bunch of very enjoyable features and awesome ideas that were brought over to be used in LittleKt.

David Wursteisen's excellent multiplatform game framework [MiniGDX](https://github.com/minigdx/minigdx/) that allowed LittleKt to get up and running quickly.
