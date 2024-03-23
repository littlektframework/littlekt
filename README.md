[![Logo](/art/logo/logo-outline.svg)](https://littlekt.com)

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/littlektframework/littlekt/blob/master/LICENSE)
[![build](https://github.com/littlektframework/littlekt/actions/workflows/build.yml/badge.svg?branch=master)](https://github.com/littlektframework/littlekt/actions/workflows/build.yml)
[![Download](https://img.shields.io/maven-central/v/com.lehaine.littlekt/core/0.8.1)](https://search.maven.org/artifact/com.lehaine.littlekt/core/0.8.1/pom)

**Currently, in development.**

**[Features](https://littlekt.com/features/)** - **[Docs](https://littlekt.com/docs/)** - **[Dokka / KDocs](https://littlekt.com/dokka/)** - **[Samples](https://github.com/littlektframework/littlekt-samples)** -**[Showcase](#showcase)** - **[Ask a Question](https://github.com/littlektframework/littlekt/discussions/categories/q-a)** - **[Changelog](CHANGELOG.md)** - **[Starter Project](https://github.com/littlektframework/littlekt-game-template)**

## A 2D game framework written in Kotlin

**LittleKt (Little Kotlin) is a Kotlin multiplatform 2D game development framework based on OpenGL** that is inspired by
libGDX and KorGE. The goal of this project is to allow the freedom and flexibility that libGDX offers with enjoyable
idiomatic features coded in Kotlin that KorGE has to offer.

Check out some planned [features](https://github.com/littlektframework/littlekt/labels/enhancement)

### Install

LittleKt releases are hosted on Maven Central and can be installed like so:

**build.gradle.kts**:

```kotlin
repositories {
    mavenCentral()
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11" // littlekt targets jvm 11 so we must target atleast 11
        }
    }
}

val littleKtVersion = "0.8.1" // get the latest release at the top
val kotlinCoroutinesVersion = "1.8.0-RC2" // or whatever version you are using

sourceSets {
    val commonMain by getting {
        dependencies {
            implementation("com.lehaine.littlekt:core:$littleKtVersion")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")  // littlekt requires coroutines library on the classpath
        }
    }
}
```

### Snapshots

On every build a snapshot gets created. If you want to be on the bleeding edge then you can pull from the snapshot repo.
The snapshot versioning uses commit hashes as a suffix. The version convention looks like so: `x.x.x.hash-SNAPSHOT`.
E.g `0.2.1.080b1ad-SNAPSHOT`.
**Note**: this will most likely cause breaking changes

**build.gradle.kts**:

```kotlin
repositories {
    maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11" // littlekt targets jvm 11 so we must target atleast 11
        }
    }
}

val littleKtVersion = "0.5.0.af4fdbf-SNAPSHOT" // or whichever hash you are using
val kotlinCoroutinesVersion = "1.6.0" // or whatever version you are using

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

| Platform                | Support |     Expected By |
|-------------------------|:-------:|----------------:|
| Desktop (JVM)           |    ✅    | Current version |
| Web (JS/WASM) (WebGL/2) |    ✅    | Current version |
| Android                 |    ✅    | Current version |
| iOS / Native            | Planned |            v1.0 |

### Showcase

Real world examples instead of the [samples'](https://github.com/littlektframework/littlekt-samples) repo.

#### Glutton for Punishment

**[Source](https://github.com/LeHaine/ggo2022)** - **[Play](https://lehaine.itch.io/glutton-for-punishment)**

![gif](https://github.com/LeHaine/ggo2022/blob/master/itchio/gif1.gif)

### Acknowledgements

LittleKt was put together based on bits and pieces of features found across multiple engines/frameworks and languages
that were very enjoyable to use and flexible. If a piece a code looks familiar, feel free to open an issue with details,
so that we can properly attribute the code.

A big thanks to the folks over on libGDX and KTX, KorGE, and MiniGDX.

The very popular and amazing [libGDX](https://github.com/libgdx/libgdx) which is the main inspiration of this framework
as well as the Kotlin framework [KTX](https://github.com/libktx/ktx) for the clever and awesome utilites and extensions
built on top of libGDX.

Carlos Velasco's (soywiz) awesome Kotlin game engine [KorGE](https://github.com/korlibs/korge) which has a bunch of very
enjoyable features and awesome ideas that were brought over to be used in LittleKt.

Max Thiele's (fabmax) incredible Kotlin OpenGL/Vulkan graphics engine [kool](https://github.com/fabmax/kool) where many
features were shamelessly copied and brought over as well which helped get many graphics related features working and
allowed me to understand how it worked.

David Wursteisen's excellent multiplatform game framework [MiniGDX](https://github.com/minigdx/minigdx/) that allowed
LittleKt to get up and running quickly.
