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
* **BREAKING**: remove: `getColorHex` and `getColorInt` methods from `LDtkIntGridLayer`.
* add: `getColor()` that returns a new `Color` instance to `LDtkIntGridLayer`.
* add: `truncate` string to `Font`, `FontCache` and `GlyphLayout`.
* add: ability to focus `Control` nodes and cycle through them with keyboard.
* add: `onFocus` and `onFocusLost` methods to `Control`
* **BREAKING**: move most UI enum classes to the specific Node class such as `SizeFlag`, `StretchMode`,
  and `AnchorLayout`
* update: `SceneGraph` to trigger ui input events for keyboard
* update: `BaseButton` to allow for triggering press signal with keyboard
* update `Button` with new _focus_ theme variable drawable
* add: `LineEdit` control for editing single lines of text.
* add: `Clipboard` support
* add: showing/hiding soft keyboard for Android
* **BREAKING**: rename `InputMultiplexer` to `InputMapController`
* add: new `InputMapProcessor` interface with `onAction` callbacks used with `InputMapController`
* update: `InputMapController` to handle `Pointer` types as a binding
* update: `InputMapController` to handle key modifiers in a binding (SHIFT, CTRL, and ALT)
* update: `SceneGraph` to use an `InputMapController` and input actions
* update: `SceneGraph` focus key binds to use action bindings
* add: helper methods to set default UI input action bindings to for `SceneGraph`
* add: `justTouched` and `touchJustReleased` methods to `Input`
* **BREAKING**: rename `onKeyTyped` to `onCharType`.
* add: new `onKeyRepeat` method to `InputProcessor`
* add: support for `KEY_REPEAT` event in `SceneGraph`
* **BREAKING**: move `StretchMode` into `TextureRect` class.
* add: `TILE` stretch mode implementation to `TextureRect`
* add: `TextureProgress` control node
* update: `NinePatch` to support setting source rectangle
* **BREAKING**: update: `NinePatchRect` control node to use a `TextureSlice` vs using a `NinePatch` directly.
* **BREAKING**: update `Scene` to use scoped lifecycle methods with `Context`
* **BREAKING**: update `Scene` to no longer inherit from `AssetProvider`
* update: `Game` with `vfs` and `clipboard` properties
* add: a return value to `Context` callback methods (`onRender`, `onPostRender`, `onResize`, `onDispose`
  , `onPostRunnable`) that can be invoked once to remove the callback from being invoked.
* add: new `SizeFlag` value of `NONE` which is the same as creating a `SizeFlag(0)`
* add: a new parameter to the `resize` method of a `SceneGraph` to allow centering of the camera if `true`.

### Bugs:

* fix: remove clearing color buffer in `LwjglContext`.
* fix: `PathInfo` to handle `./`.
* fix: `InputQueueProcessor` from triggering any subsequent input processors if the input has been handled
* fix: `InputQueueProcessor` not correctly resetting internal input events to be reused.
* fix: `Pool` from freeing an allocated object when using a callback.
* fix: `TextureSlice` using incorrect UV coordinates for a 1x1 slice
* fix: LWJGL input not resetting the last char when typing
* fix: `TextureRect` not actually using the specified width and height for stretching
* fix: `SceneGraph` focusing a `Control` node that wasn't enabled
* fix: `Node` not updating children nodes when `enable` value was changed.
* fix: `Button` sometimes not calculating text size resulting in misplaced text
* fix: `Label` sometimes not calculating text size resulting in misplaced text

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