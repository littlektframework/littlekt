# Changelog

## v0.2.0

* add: **MutableTextureAtlas** class to combine existing `TextureAtlas`, `Texture`, and `TextureSlice` types into a
  single `TextureAtlas`.
* update:`vfsFile.readBitmapFont()` loader to allow passing in a list of existing `TextureSlice`, such as from
  a `TextureAtlas`.
* update: `core` now includes `tools` as an **api** vs just **implementation. Required for the new `MutableTextureAtlas`
  class.

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