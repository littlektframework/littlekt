# LittleKt - An OpenGL game framework written in Kotlin
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/LittleKtOrg/LittleKt/blob/master/LICENSE)

A mutliplatform lower level game framework inspired by libGDX and KorGE. The goal of this project is to allow the freedom and flexibility that libGDX offers with enjoyable idiomatic features coded in Kotlin that KorGE has to offer.

Check out the [samples](https://github.com/LittleKtOrg/LittleKt-Samples) repository.

Documentation to come at a later time.

### Features
* **Freedom and flexibiity**: Various useful tools and abstractions but still allow access to the underlying code.
* **Kotlin**: 100% written in Kotlin which allows to take advantage of all the great features the language offers.
* **Cross-platform**: Thanks to Kotlin - building for multiple platforms is easy. 
* **Font**: Loaders and renders to make font's easy to use
  * TTF file parser and loader
  * GPU vector font rendering of TTF files on the fly based off of [Will Dobbie's article](https://wdobbie.com/post/gpu-text-rendering-with-vector-textures/)
* **High-Level 2D APIs**:
  * Orthographic camera and viewports
  * Texture atlases, slicing, and pixmap editing 
  * Shader GLSL generation DSL
* **Low-Level OpenGL utilities**:
  * Shaders 
  * Meshes
  * Textures
  * Framebuffer objects
  * Vertex arrays and vertex buffer objects
* **Tile maps**:
  * Full [LDtk](https://ldtk.io) map loading and rendering
* **Virtual File System**:
  * A file system abstraction that allows for easy access for reading and writing files
  * A flexibile asynchronous asset provider system 
* **Math**:
  * Matrix and vector classes
  * Bounding shapes and Bresenham implementations
  * Interpolators
* **Utilities**:
  * Helper extensions and quality-of-life util functions to make writing code very enjoyable 
* With lots more planned and to come. Check out some of the planned [features](https://github.com/LittleKtOrg/LittleKt/labels/enhancement)

### Cross-platform 

| Platform | Implemented |
| -------- | :---------: |
| Desktop (JVM) | ✅ |
| Web (WebGL/2) | ✅ |
| Android | Not started |
| iOS | Not started |
| Desktop (Native) | Not started |

### Acknowledgements
LittleKt was put together based on bits and pieces of features found across multiple engines/frameworks and languages that were very enjoyable to use and flexible. If a piece a code looks familiar, feel free to open an issue with details, so that we can properly attribute the code.

A big thanks to the folks over on libGDX, KorGE, and MiniGDX.

The very popular and amazing [libGDX](https://github.com/libgdx/libgdx) which is the main inspiration of this framework.

Carlos Velasco's (soywiz) awesome Kotlin game engine [KorGE](https://github.com/korlibs/korge) which has a bunch of very enjoyable features and awesome ideas that were brought over to be used in LittleKt.

David Wursteisen's excellent multiplatform game framework [MiniGDX](https://github.com/minigdx/minigdx/) that allowed LittleKt to get up and running quickly.
