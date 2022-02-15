# Changelog

## v0.2.0 (Current SNAPSHOT)

### New

* add: Full **Android** support.
* add: `MutableTextureAtlas` class to combine existing `TextureAtlas`, `Texture`, and `TextureSlice` types into a
  single `TextureAtlas`.
* update:`vfsFile.readBitmapFont()` loader to allow passing in a list of existing `TextureSlice`, such as from
  a `TextureAtlas`.
* update: `core` now includes `tools` as an **api** vs just **implementation. Required for the new `MutableTextureAtlas`
  class.
* JVM config: add `backgroundColor` option for initially setting background color.
* JS config: add `backgroundColor` option for initially setting background color.
* add: `Batch` interface.
* update: `SpriteBatch` to use new interface.
* update: all references to `SpriteBatch` with `Batch`.
* add: New batch implementation called `TextureArraySpriteBatch` that uses texture arrays to render multiple textures as
  a single draw call (GL 3+).
* add: new helper methods for projecting / un-projecting coordinates on a `Camera`.
* update: `GLVersion` to handle parsing version for **OpenGL ES**.
* update: `Pointer` enum to support more than **3** pointers / touches (due to Android support).
* update: `Pointer` with an `index` parameter for determine the actual index of the pointer / touch.
* update: `Input` with new `vibrate()` and `cancelVibrate()` methods (Android support).
* update `GLSLGenerator` to handle ES versions for mobile platforms.

### Bugs:

* fix: remove clearing color buffer in `LwjglContext`.
* fix: `PathInfo` to handle `./`.

### Upgrades:

* `Kotlin` from `1.6.0` to `1.6.10`
* `LWJGL` from `3.2.3` to `3.3.0`
* `kotlinx-coroutines` from `1.6.0-RC` to `1.6.0-native-mt`
* `kotlinx-serialization` from `1.3.1` to `1.3.2`

## v0.1.0

* Add **JVM** support
* Add **JS** support
* Add **SceneGraph** with **Node** and **Node2D** implementations
* Add **UI** built on top of **SceneGraph**
    * **Controls**:
        * Button
        * Label
        * Panel
        * TextureRect
        * NinePatchRect
        * ProgressBar
    * **Containers**:
        * VBoxContainer and HBoxContainer
        * PaddedContainer
        * PanelContainer
        * CenterContainer
* Add **Virtual File System**
* Add **Shader** support
* Add **GLSL Generator** for building shaders with Kotlin
* Add **Texture Atlas** support
* Add **Bitmap Font** support
* Add **Audio** streaming support
* Add **Audio** clips support
* Add **math** module
    * Vectors
    * Matrices
    * Angles
    * Point
    * Bresenham
* Add sprite batching
* Add **Scene** class
* Add **Game** class for managing scenes
* Add **Asset provider** class to help load and prepare assets asynchronously
* Add **App** and **Engine** stats
* Add coroutine and async support and utilities
* Add **LDtk** tilemap support
* Add **TTF** file font parsing and loading
* Add **GPU font** rendering
* Add bin packer **max rects** algorithm
* Add texture packer tool
* Add **Input** processing with mouse, keyboard, and gamepad
* A **Camera** and **Viewport** support
* Add **Logger**
* Add **Frame buffer objects** support
* Add **Particles** support
* Add **Animation** support
* Add **Color** class