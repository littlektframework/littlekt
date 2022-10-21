# Changelog

## v.0.5.0 (Current SHAPSHOT)

### Highlights

* Add `ScrollContainer`, `VScrollBar`, and `HScrollBar` control nodes for scroll handling

### Breaking

* Add `ShapeRenderer` to `SceneGraph` and `Node` render methods. This includes `preRender`, `render`, `postRender`
  and `debugRender` as well as all the `Node` rendering signals.

### New

* add: `EmptyDrawable` to handle drawables that don't need drawn.
* add: Additional vec3 `CanvasLayer.canvasToScreen` methods.
* add: two new size properties to `NinePatch`: `totalWidth` & `totalHeight`.
* add: new `Textures.transparent` 1x1 transparent pixel texture slice.
* add: `MutableVec2f.mul(Mat4)` implementation.
* add: `Node.moveChild(child, index)` method to handle moving a child to different index in the tree.
* add: `SceneGraph` now propagates scroll input to control nodes that the mouse is over.
* add: `allowGreater` & `allowLesser` flags to `Range` node to allow values pass the min & max values.

### Changes

* fix: `SceneGraph` sometimes adding handled input to the unhandled input queue.
* update: `Theme.FALLBACK_DRAWABLE` is not an `EmptyDrawable` instead of a white `TextureSliceDrawable`.
* update: `Control` now checks to ensure node is `visible` before returning successfully in the `hit` test.
* update: `NinePatchDrawable` `minWidth` and `minHeight` calculations to use the new `NinePatch.totalWidth` and `NinePatch.totalHeight` properties.
* fix: `PanelContainer` not fitting children nodes to the correct positions.
* update: default UI asset pngs.
* update: `Label` default `mouseFilter` is now `IGNORE`.
* update: `Control.setAnchor` to be public.

## v.0.4.0

### Highlights

* `SceneGraph` nodes now allow for custom sort options when updating lists. This allows to render nodes based on the
  sort while keeping the update order.
* `SceneGraph`: New `ySort` option added to `Node2D` to allow y-sorting rendering.
* `SceneGraph` will now handle debug rendering as well as `Control` nodes to render it's bounds when rendering to help
  UI positioning.
* `JVM`: Added `arm64` dependencies for LWJGL.

### Breaking

* Add `ShapeRenderer` to `SceneGraph` and `Node` render methods. This includes `preRender`, `render`, `postRender`
  and `debugRender` as well as all the `Node` rendering signals.

  #### Migration:

  Before:

  **DSL:**

    ```kotlin
    node {
        onRender += { batch, camera ->
            // render logic
        }
    }
    ```

  **Extending a Node:**

    ```kotlin
    override fun render(batch: Batch, camera: Camera) {
        // render logic
    }
    ```

  New:

  **DSL:**

    ```kotlin
    node {
        onRender += { batch, camera, shapeRenderer ->
            // render logic
        }
    }
    ```

  **Extending a Node:**

    ```kotlin
    override fun render(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
        // render logic
    }
    ```

### New

* add: `Camera.boundsInFrustum` implementation.
* add: New implementation of `AnimationPlayer.start()` that will restart the current running animation to the first
  frame. `AnimationPlayer.resume()` provides the old functionality of `AnimationPlayer.start()`
* add: `AnimationPlayer.restart()` alias for `AnimationPlayer.start()`.
* add: `Experimental` annotation that requires opt-in.

### Changes

* fix: add missing `contract` call for `vBoxContainer`.
* fix: JVM - add a `isMac` check before attempting to set window icon.
* fix: Texture Packer Plugin - All LWJGL dependencies are now removed from the plugin.
* update: Refactor `AnimationPlayer.start()` to `AnimationPlayer.resume()`.
* update: `AnimationPlayer` restarts the last frame time back to zero when stopping an animation.
* update: `GpuFont` is now marked as `Experimental`.
* update Android compile & target versions to 33.
* update `AssetProviderer` to use an `ArrayList` instead of a `MutableList` to fix iterating through assets to prepare.

## v0.3.2

### Bugs

* fix: Infix function `ife` from using the incorrect `isFuzzyEquals` method resulting in incorrect values.

## v0.3.1

### Changes

* update: `Viewport.camera` to be mutable.
* update: all existing `Viewport` subtype classes to receive a constructor paramter for a `Camera`.

## v0.3.0

### Highlights

**For the full log see below.**

* New `SceneGraph` nodes to handle different viewports, frame buffers, and materials (shaders, blend modes,
  depth/stencil modes).
* New input and unhandled input handling inside a `SceneGraph`.
* New lifecycle methods added to Nodes in a `SceneGraph`: `preUpdate()`, `postUpdate()` and `fixedUpdate()`.
* Major optimizations and clean up of the `SceneGraph` in general.
* `Camera` & `Viewport` relationship refactoring to make more sense. See full changelog for more info.
* `AnimationPlayer` playback clean up and add few additional playback methods
* New optional `AnimationStateMachine` within `AnimationPlayer`.
* New shape, lines, and path drawing class `ShapeRenderer`.

### Breaking

* refactor `Viewport` to contain an internal `Camera` and remove `Viewport` from `Camera`.

  #### Migration:

  Before:

    ```kotlin
    val viewport = ExtendViewport(480, 270)
    val camera = OrthographicCamrea().apply {
        this.viewport = viewport
    }
    ```

  New:

    ```kotlin
    val viewport = ExtendViewport(480, 270)
    val camera = viewport.camera
    ```

* remove `SceneGraph.viewport: Viewport` and `SceneGraph.camera` and replaced with the `ViewportCanvasLayer` node.

  #### Migration:

  Before:

    ```kotlin
    val viewport: Viewport = graph.viewport
    val camera: Camera = graph.camera
    ```

  After:

    ```kotlin
    val viewport: Viewport = graph.sceneCanvas.viewport
    val camera: Camera = -graph.sceneCanvas.canvasCamera
    ```
* update `SceneGraph.initialize()` to be **suspending**.
* update `SceneGraph.Node.initialize()` to be **suspending**.
* rename `Node2D.toWorld` functions to `toGlobal`.
* update `Vec2f.rotate` to use `Angle` instead of a float.

  #### Migration:

  Before:

    ```kotlin
    val vec = MutableVec2f()
    val angle: Angle = 45.degrees
    vec.rotate(angle.degrees)
    ```

  After:

    ```kotlin
    val vec = MutableVec2f()
    val angle: Angle = 45.degrees
    vec.rotate(angle)
    ```

### New

* add: list of catching keys to prevent default platform functionality from performing on key events
* add: `loadSuspending` to `AssetProvider` to load assets as a suspending function instead loading inside a coroutine
* add: `onDestroy` open function and `onDestroy` signal to `Node` when `destroy()` is invoked.
* add: contracts to `SceneGraph` DSL to allow usage of assigning objects to `val` outside of it.
* add: contract to `AssetProvider.prepare()` to allow usage of assigning objects to `val` outside of it.
* add: `CanvasItem` node as the base node for 2D transformations in the `SceneGraph`. `Node2D` now inherits directly
  from this node.
* add: `CanvasLayer` node that creates a separate `OrthographicCamera` for rendering instead of inheriting the base
  camera from the `SceneGraph`. For example: When rendering certain nodes at a low resolution using a `Viewport` to
  render UI at a higher resolution. This contains an instance to a `Viewport` but isn't used directly to render
  children. A new node called `ViewportCanvasLayer` can be used to render children nodes with a separate viewport.
* add: `ViewportCanvasLayer` to use separate `Viewport` to render children nodes in a `SceneGraph`.
* add: `FrameBufferNode` which is a `CanvasLayer` to allow rendering children nodes directly to a `FrameBuffer`.
  The `FrameBuffer` texture is then available to be rendered in a separate node.
* add: `FrameBufferContainer` control node which can be used to help render a `FrameBufferNode`.
* add: `zoom`, `near`, and `far` properties to `Camera2D` which will be used to set the `CanvasLayer.canvasCamera`
  properties before rendering.
* add: `scale` property to the `render` method of `LDtk` and `Tiled` tile maps.
* add: `tmod` and `targetFPS` properties to `SceneGraph` to be used as another way to handle frame independent
  movements.
* add: **three** new lifecycle calls to a `Node`: `onPreUpdate`, `onPostUpdate`, and `onFixedUpdate` as well as their
  respective `Signal` types. The `onFixedUpdate` comes with a few more properties to handle changing the fixed time step
  and as well as an interpolating ratio.
    * `SceneGraph.fixedTimesPerSecond`: can be used to set the amount of times `onFixedUpdate` is call per second
    * `SceneGraph.fixedProgressionRatio`: is an interpolation ratio that can be used to render nodes that
      use `onFixedUpdate` for movement / physics.
* add: `input` and `unhandledInput` callbacks to all `Node`.
    * `input`: is called whenever there is an `InputEvent` generated and is propagated up the node tree until a node
      consumes it.
    * `unhandledInput`: is called when an `InputEvent` isn't consumed by `input` or the UI.
* add: `resizable`, `maximize`, and `windowPosition` configuration parameters to JVM.
* add: `ppu` and `ppuInv` properties to `SceneGraph` and `Node` that allow setting the **Pixels per Unit** which is used
  for rendering.
* add: `Material` class and property to `CanvasItem` that can be used to set and change shaders, blend modes, and
  depth/stencil modes.
* add: `AnimationState` optional state machine stack that can be to register animation states that will be played based
  on a predicate.
* add: `CropType` options to `TexturePacker`: `NONE`, `FLUSH_POSITION`, and `KEEP_POSITION`.
* add: `ShapeRenderer` class to handle drawing shapes, lines, and paths using a `Batch` instance and a `TextureSlice`.

### Changes

* update: `SceneGraph` methods `resize()`, `render()`, and `initialize()` as `open`.
* update: `AnimationPlayer` to handle the way animations are queued and played back based on a stack.

### Bugs:

* fix: clearing signals of all existing nodes when destroyed.
* fix: `LDtk` auto tile maps now only loop through tiles within the view bounds instead of all the tiles.

## v0.2.0

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
* update: `Context` callbacks (`onRender`, `onPosRender`, `onResize`, `onDispose`, and `postRunnable`) to return
  a `RemoveContextCallback` lambda that can be invoked to remove itself from being called by the `Context`.
* update: the `resize()` method in a `SceneGraph` to allow optional centering of camera.
* update: **LDtk** version support to `1.0.0 beta3`
* **BREAKING**: remove `readLDtkLevel` from `VfsLoaders`.
* **BREAKING**: refactor `readLDtkMap` to `readLDtkMapLoader`. This now returns the `LDtkMapLoader` which then can be
  used to call `loadMap()` and `loadLevel()`.
* **BREAKING**: remove `LDtkWorld` and `LDtkLevel` from `AssetProvider` default loaders
* add: Passing in optional `TextureAtlas` when reading an `LDtkMapLoader`. Requires preloading tileset textures in order
  to benefit from it.
* add: **Tiled** map support. Includes, **orthographic**, **isometric**, and **staggered** map rendering support.
* update: `Rect` class is now **open**.
* add: `extrude`, `bleed` and `bleedIterations` to `TexturePacker` to prevent atlas bleeding and prevent filtering
  artifacts when RGB values are sampled for transparent pixels.
* add `TexMinFilter` and `TexMagFilter` constructor params to `FrameBuffer`

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
* fix: `GlyphLayoutRun` not correctly calculating next glyph `advance` value.
* fix: `Button` text width calculations due to `GlyphLayoutRun` glyph `advance` fix
* fix: `LWJGL` application defaulting to graphic cards `vSync` setting when `vSync` was set to false. It now will
  properly turn off `vSync`.
* fix: `AnimationPlayer` not able to restart a current animation by using `stop()` and then `play()` without having to
  swap animations.

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