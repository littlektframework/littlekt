# Changelog

## 0.11.0 (SNAPSHOT)

- BREAKING: Update minimum JDK from 21 to 22. `--enable-preview` is no longer needed.
- Update `wgpu` from `0.19.4.1` to `22.1.0.5`.
- BREAKING: `rect.interescts(left, top, right, bottom)` has been reorder to a more natural way:
  `rect.intersects(left, bottom, right, top)`
- Fix `rect.intersects(rect2)` to pass in correct coordinates.
- Fix indices & vertices being overwritten, when they shouldn't be, when ensuring buffer size inside `Mesh` and
  `IndexedMesh`.
- Fix `JsByteSequenceStream` from attempting to flip the passed in `Buffer` in order to read. It now assumes the buffer
  is in a ready-to-ready state.
- Add new stats to `EngineStats`:
    - `setPipeline` calls per frame
    - `setBindGroup` calls per frame
    - `set*Buffer` calls per frame

## 0.10.2

### Changes

- Fix `Rect.intersects` to use correct top & bottom coordinate during calculation.
- Fix `InputMapController` bindings are triggered without modifiers when a modifier is pressed.
- Fix jvm `AudioStream` volume not being able to be set to `0`.
- Update `TiledLayer.visible` to be mutable.
- Add support for object within a tile in `Tiled` maps
- Add fetching tile ID by coordinates in `TiledLayer`.
- Fix GamePad button axis strength calculation not using the negative input resulting in buttons not affecting axis
  strength
- Fix `Animation.getFrame(index)` and `Animation.getFrameTime(index)` from being able to go out index bounds.
- Add new `Vfs` subtypes: `UrlVfs` and `LocalVfs`.
    - `Context` now has three `Vfs` types:
        1. `resourcesVfs` for loading from the `resources` directory. This handles loading from fat JARs fine now (fixes
           #275)
        2. `urlVfs` for loading directly from a URL or data URL.
        3. `applicationVfs` for loading files at the root of the application working directory.
- Update internal JVM `readPixmap` to use `stbimage` instead of `ImageIO`
- Fix `IndexedMeshGeometry` index buffer size calculation
- Add `Compression` interface with `CompressionGZIP` implementations for jvmAndroid & js.
- Fix `AssetProvider.fullyLoaded` calculation to take into account the active job
- Update `kotlin` from `2.0.0` to `2.1.0`
- Update `kotlinx.serialization` from `1.7.0` to `1.7.3`
- Update `kotlinx.atomicfu` from `0.24.0` to `0.26.1`
- Update `kotlinx.coroutines` from `1.9.0-RC` to `1.9.0`
- Update `LWJGL` from `3.3.3` to `3.3.4`
- Internal code clean up
- Documentation tweaks and clean up

## 0.10.1

### Changes

- Fix miscalculation for **scene-graph** `Cotnrol.anchor()` for `TOP_*` related layouts.
- Fix `AssetProvider` to asset preparation logic not preparing additional assets after the initial preparation.

## 0.10.0

### Breaking

* Replace all of **OpenGL** with **WebGPU**. The overall API is mostly the same, save for the specifics of having to use
  WebGPU. These changes are too large to fit in a changelog but can be checked out on the documentation and migrated.
    * Much of the API under `graphics.*` has changed but stayed relatively the same. E.g. `SpriteBatch` contains the
      same `begin() -> draw() -> end()` flow but requires WebGPU specific classes.
    * `graphics.gl.*` classes replaced with `graphics.webgpu.*`
* Remove the Android target until a generator can be created for the WGPU natives for JNI (Future update).
* The module group id, for defining in dependencies, has been changed from `com.lehaine.littlekt` to `com.littlekt`.
* The **scene-graph** module has been extracted into its own package and will explicitly be defined in your
  dependencies: `com.littlekt:scene-graph`
* Remove `FrameBufferNode` and related UI classes. Use `CanvasLayerContainer` and `CanvasLayer` for FBO related
  purposes.
* Remove `FrameBuffer` class. WebGPUs `RenderPass` is essentially a Framebuffer / Render target.
* Remove `GlslGenerator` and all related classes. Use `WGSL` for your shader needs, either by loading them from a file
  or directly in a string.
* Remove `vSync` and `backgroundColor` configuration options.
* Remove `Game<T>` and `Scene<T>` classes as they aren't in scope of the framework.
* Rename `Disposable` to be `Releasable` as well as `dispose()` to `release()`.
* Remove `FitViewport` and `FillViewport` as they don't work with the current viewport limitations WebGPU imposes.
  WebGPU doesn't allow out-of-bounds viewports which causes a fatal error. There is no workaround without getting clever
  with shaders.
* Update `TextureSlice.originalWidth/Height` & `TextureSlice.packedWidth/Height` to `actualWidth/Height`
  and `trimmedWidth/Height`, respecitively.

### Changes

* Add new `wgpu-ffm` module.
* Add new `wgpu-natives` module.
* Add new `SpriteCache` renderer.
* Update Kotlin to `2.0.0`
* Update `Kotlin.coroutines` to `1.9.0-RC`
* Update `Kotlin.atomicfu` to `0.24.0`
* Update `Kotlinx.serialization` to `1.7.0`
* Update `Kotlinx.html` to `0.11.0`
* Add documentation to most of the framework.
* Dozens of other minor misc. changes all across the framework.

### Fixes

* Dozens of fixes related to the **scene-graph**:
    * Layout miscalculations
    * Optimizations
    * Scrolling fixes
    * Scissor fixes
* Fix LDtk tilemap rendering issues
* Fix Tiled tilemap rendering issues
* Dozens of other minor misc. fixes all across the framework.

## 0.9.0

### Breaking

* `ShaderProgram.getAttrib()` can now return a `-1` instead of throwing an `IllegalStateException` if the attribute name
  doesn't exist.
* `ShaderProgram.getUniformLocation` can now return a `null` instead of the `UniformLocation` if the uniform name
  doesn't exist.
* Update JVM target to 17 from 11.

### Changes

* Add WASM as an official platform target.
* Update `FrameBuffer` to allow for multiple texture attachments to be used.
* Remove nest-ability from `FrameBuffer` due to poor performance with multiple calls to get currently bound frame
  buffer.
    * `FrameBuffer.end()` will now bind to the default frame buffer instead with optional viewport position and size
      parameters.
* Update `GLSlGenerator` to support `gl_FragData[]`.
* Update `GL` with new `drawBuffer` and `clearBuffer[fuiv]` functions.
* Update `GL` with new `getActiveAttrib` and `getActiveUniform` related functions.
* Optimize `WebGL` to prevent creating a new array on certain `GL` calls.
* Update `ShaderProgram` to handle finding and setting active uniforms and attributes instead relying on
  the `ShaderParameter` to do so.
* Update `AGP` to `8.2.0`.
* Update Kotlin to `1.9.23`.
* Update `kotlinx-coroutines` to `1.8.0`.
* Update Dokka gradle plugin to `1.9.20`.

### Fixes

* Fix `InputQueueProcessor` to clear events pool even when no `InputProcessor` exists.
* Fix `SpriteBatch` default size to be back to 1000 sprites instead of 8191 sprites.
    * This makes it consistent with `TextureArraySpriteBatch` as well as increases performance if using the default size
      and using less than 8191 sprites.

## 0.8.1

### Changes

* Remove remaining 3D spatial graphic classes that were accidentally left over from `0.8.0`.
* Update LDtk to handle changes up to LDtk `1.5.3`.
* Update Kotlin to `1.9.21`.
* Update `Kotlinx.coroutines` to `1.8.0-RC2`.
* Update `Kotlinx.atomicfu` to `0.23.1`.
* Update `Kotlinx.serialization` to `1.6.2`.
* Update Dokka gradle plugin to `1.9.10`.

### Fixes

* Fix `NoSuchElementException` being thrown when testing touch input in browsers simulated device-mode.

## 0.8.0

### Breaking

* Removed all experimental GLTF and 3D rendering graphics.

### Changes

* Update Kotlin to `1.9.10`.
* Update `Kotlinx.coroutines` to `1.7.3`.
* Update `Kotlinx.atomicfu` to `0.22.0`.
* Update `Kotlinx.serialization` to `1.6.0`.
* Update `LWJGL` to `3.3.3`.
* Update Android Gradle plugin to `7.3.1`.
* Update Gradle Version (gver) plugin to `0.48.0`.
* Update Dokka gradle plugin to `1.9.0`.

### Fixes

* Fix `Button` label not calculating layout correctly when adding directly to a `Control` node.

## v0.7.0

### Changes

* Update Kotlin to `1.8.20`.
* Update `FrameBuffer` class is now `open`.
* Add new `forEachTileInView` to `LdtkLayer` to iterate over tiles current in the view bounds without rendering.
* **Breaking**: Move `BlendMode` and `DepthStencilMode` out of the `graph` package and into the `graphics.utils`.
* Add new `setBlendFunction(blendMode)` to `Batch`.
* **Breaking**: update `createShader` to return the type of vertex and fragment shaders instead of the super class.
* Add `FrameBuffer.use` extension similar to `Batch.use`.
* **Breaking**: `Shader.parameters` are now to be used with a `LinkedHashSet` instead of
  a `MutableList()`. `mutableListOf(param1, param2) -> linkedSetOf(param1, param2)`.
* **Breaking**: Update `Shader.parameters` to grab parameters by variable name instead of
  index. `parameters[0] -> parameters["u_texture"]`
* **Breaking**: `GlslGenerator.texture2D`, `GlslGenerator.shadow2D`, and `GlslGenerator.texture` now returns a
  constructor delegate instead of a literal. `val color = texture2D(...) -> val color by texture2D(...)`

### Fixes

* Fix `TextureArraySpriteBatch.draw(spriteVertices)` to manually increase `Mesh.geometry.numVertices` to prevent data
  from being overwritten with `geometry.addVertex`.
* Fix `TextureArraySpriteBatch` to use the correct `VertexAttribute` for 2D positions.
* Fix `TextureArraySpriteBatch.maxVertices` calculation.
* Fix `LDtkLayer` rendering to calculate the correct maximum cells in both x & y axes.
* Fix `SceneGraph` to set the correct blend equation off a material `BlendMode`.
* Fix `GlslGenerator` not removing unused definitions from functions.
* Fix `Tiled` row & column calculations & iso transform to use correct view bound points.
* Fix `GlslGenerator.For` not using `GLInt` value directly
* Fix `GlslGenerator.atan` to use correct parameters (was only allowing one parameter to be passed in)
* Fix `InputMapController.addBinding` to check for key modifiers for `down()`, `pressed()`, and `released()` functions

## v0.6.3

* Fix an issue with `Button` not calculating label sizing when added after scene creation.

## v0.6.2

This contains only a single change related to the `GLSLGenerator`.

* Update `GLSLGenerator` uniform delegates to only add to the `Shader.paremeters` only when has its value used in the
  source. Due to some static conditions some uniforms may not be used while still being defined in the `paramters` list.
  When creating a `ShaderProgram` with WebGL, it would throw shader compilation error due to not finding in the glsl
  source. This was preventing a `SceneGraph` from being rendered on the Web due to the default 3D model shader.

## v0.6.1

This contains fixes for Mac support.

### Fixes

* Add new HdpiMode for JVM platforms. Setting to HdpiMode.PIXELS will allow correct display on Macs with retina
  displays.
* Update GlVersion to handle comparisons of versions correctly. This fixes issues in the `GlslGenerator`.
* Update `LjwglGL` to only use the extension framebuffer and renderbuffer calls if the OpenGL version is `< 3`.
* Fix shader compilation errors on Macs.

## v0.6.0

This version is full of breaking changes, mainly to mesh creation and handling, to make way for adding some basic 3d
related graphics and handling.

### Highlights

* Basic 3D support **This is experimental and not really meant to be used in a real game yet**:
    * Adds a few new nodes to the `SceneGraph` to handle rendering 3D meshes and models:
        * `Node3D`, `VisualInstance`, `Model`, and `Camera3D`.
    * Extremely basic lighting (this will improve in the future).
    * Skeletal animation.
    * glTF loading and basic material rendering: `resourceVfs["myModel.glb"].readGltfModel()`.
    * New `PerspectiveCamera` class.
* New `MeshGeometry` class. Replaces the old `MeshBatcher` but doesn't require to make a new `Mesh` instance to create
  the vertices. An instance can be passed into a `Mesh` for it to use.
* New `MeshBuilder` class that uses the new `MeshGeometry` class to help facilitate generating meshes and vertices.
* Various GLSL generator improvements and optimizations
    * Declare and set value of variable in line.
    * 3.3 layout locations support.
    * Add new `predicate` option when declaring an `attribute` or `varying` for conditional shaders.
* `Vec4` updates to handle `Quaternion` related math

### Breaking

* Update `BaseButton.buttonGroup` to be private and added a new `setButtonGroup(group)` function to handle setting
  button groups.

  #### Migration:
  **Previous:**
  ```kotlin
  val myButtonGroup = ButtonGroup()
  button {
    buttonGroup = myButtonGroup
    buttonGroup.buttons += this
  }
  ```
  **New**:
  ```kotlin
  val myButtonGroup = ButtonGroup()
  button {
    setButtonGroup(myButtonGroup)
  }
  ```

* Move all 2D graphic related items to new subpackage call `g2d`. To migrate: append `.g2d` to all imports using any 2D
  related class.
* Mesh changes:
    * Removed `MeshBatcher` and replaced with separate `MeshGeometry`.
    * If you are using a `Mesh` instance that was using the batcher and making use of `Mesh.addVertex` do the following:
        * Previously: `mesh.addVertex { ... }`. New: `mesh.geometry.addVertex { ... }`
    * Removed `useBatcher` in favor of using `MeshGeometry` to create a `Mesh` without binding it.
* `SceneGraph.showDebugInfo` is no longer able to be changed. To request debug rendering use the
  new `SceneGraph.requestShowDebugInfo`. The reason for this change was the `showDebugInfo` was taking effect
  immediately and throwing exceptions due to the internal `Batch` not being ready to render with `ShapeRenderer` and
  such. The request will take into effect after the current frame finishes rendering. `SceneGraph.showDebugInfo` will
  contain the true value of debug rendering.

### Changes

* Fix `BaseButton` `pressed` & `disabled` properties incorrectly comparing new value with a wrong value affecting toggle
  buttons.
* Update `NodeList` to handle `Node.updateInterval` values less than `1`. Anything less than `1` will now not update
  itself or its children.
* Add `useOriginalSize` option to `TextureRect` node to allow positioning the `TextureSlice` based off the original size
  before packing and trimming.
* Fix `SceneGraph` touch focuses not being cleared after a `TOUCH_UP` event being fired resulting in thousands of
  elements in the list.
* Fix `SceneGraph` to fire the correct `MOUSE_EXIT` input event with pointers last over an input control.
* Update `Control.hasPoint` to handle rotated control nodes
* Fix `CanvasItem.toLocal` and `CanvasItem.toGlobal` to use its respective matrices for calculation of the coordinates.
* Fix `Control.hit` not handling rotated nodes in its calculation
* Update all checks of `rotation == Angle.ZERO` to use a normalized angle and fuzzy zero checking. This results in
  angles of `360` degrees to satisfy the condition.
* Fix `TOUCH_UP` event not being sent to `input` & `unhandledInput` functions when not clicking on a `Control` node.
* Fix multiple calls to `debugRender` in the `SceneGraph` when using any nested `CanvasLayer`.
* Fix `FrameBufferNode` not propagating debug render calls to its children.
* Add new `InputEvent.canvasX` and `InputEvent.canvasY` to get coordinates of the event in the `CanvasItem.canvas`
  coordinates. This is useful when nesting `Canvas` nodes.
* Update existing `Control` nodes that use `uiInput` to check against new `InputEvent.canvasX` and `InputEvent.canvasY`
  coordinates.
* Fix `GlyphLayout` not calculating glyph advances properly resulting in incorrect wrapping calculations
* Fix when a `Control` node should calculate its minimum size. This fixes issues with labels not using the correct
  layout height.
* Add `getOrNull(Int)` to all variants of `ArrayList` data structure.

### Libraries

* Update `kotlin` to `1.8.10`.
* Update `kotlinx.serialization` to `1.5.0-RC`.
* Update `kotlinx.atomicfu` to `0.19.0`.

## v0.5.3

### Changes

* Fix `NodeList` not handling removing children correctly, missing `onDestroy` calls and not removing every child.

## v0.5.2

### Changes

* Fix `NodeList` not adding newly added nodes to the internal `sortedNodeList` preventing nodes from being rendered.

## v0.5.1

### Breaking

* Update `EmptyDrawable` to be an immutable singleton object instead of a class. Any attempted changes to this will
  throw an exception.
    * Migration: `val drawable = EmptyDrawable()` to `val drawable = createEmptyDrawable()`
      or `val drawable = EmptyDrawable`

### Changes

* Update `ScrollContainer` vertical scrollbar to extend the entire height of the container.
* Update `ScrollContainer` add new `Drawable` properties that can be overridden.
* Update `Scrollbar` add new `Drawable` properties that can be overridden.
* Fix `Scrollbar` grabber height and position not fully reaching the correct position at the end of the scrollbar.

* Adds new child tree positioning methods. NodeList updated to use just an array list to handle the indexing as the
  multiple collections were not able to handle the movement changes.
    * `sendChildToTop()` - sends child to top of parent tree
    * `sendChildAtToTop()` - sends child at index to top of parent tree
    * `sendChildToBottom()` - sends child to bottom of parent tree
    * `sendChildAtToBottom()` - sends child at index to bottom of parent tree
    * `addChildAt()` - adds new child to parent node at specified index
    * `swapChildren()` - swap child positions in the parent tree
    * `swapChildrenAt()` - swap children node positions at indices in the paren tree

## v.0.5.0

### Highlights

* Add `ScrollContainer`, `VScrollBar`, and `HScrollBar` control nodes for scroll handling
* Add automatic caching of `Control` theme values when first grabbing the theme value.
    * **For Example**: `getThemeDrawable()` will return a result and then is cached in the `drawableCache` map. Any
      subsequent calls to `getThemeDrawable()` will first check in the `drawableOverrides` and then in
      the `drawableCache` map and return a result if it exists.
    * When a theme owners theme changes, the theme owners and its child control nodes will all have their caches
      cleared.
* Add an experimental API for rendering scalable TrueType Fonts (TTF) called `VectorFont`. A sample `VectorFontTest`
  exists for usage.
* Add a `GestureProcessor` interface with a `GestureController` input processor to handle gesture detection and
  callbacks. Comes with a `GestureProcessorBuilder` class for easy callback creation. A helper
  extension `Input.gestureController` to easily create and add the gesture controller as an input processor to the
  input.
* Update default theme colors and tweak default UI assets to be slightly more presentable.
* Add `column` and `row` DSL aliases for `VBoxContainer` and `HBoxContainer` containers.

### Breaking

* `Context.onRender`, `Context.onPostRender`, `Context.onPostRunnable`, and `Context.onResize` are now non-suspending.
  This fixed issues with the
  Android and Web platforms from creating unneeded coroutines every frame. Wrap any suspending calls called within these
  callbacks in a coroutine, `KtScope.launch { }`, or check if the suspending function can be made non-suspending.

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
* update: `NinePatchDrawable` `minWidth` and `minHeight` calculations to use the new `NinePatch.totalWidth`
  and `NinePatch.totalHeight` properties.
* fix: `PanelContainer` not fitting children nodes to the correct positions.
* update: default UI asset pngs.
* update: `Label` default `mouseFilter` is now `IGNORE`.
* update: `Control.setAnchor` to be public.
* fix: `Dispatchers` coroutines attempting to resume an already cancelled continuation.
* fix: Android input not properly detecting the correct touch event action when touching with the 2 or more pointers.
* fix: LWJGL & JS input from not properly outputting the correct `Pointer` on `touchDragged` when using any pointer but
  the left mouse button.
* fix: `FontCache` not resetting its current tint when calling `clear()`.
* fix: `Label` bottom vertical alignment calculation not being correct.
* fix: `Label` horizontal alignment calculations not being correct.
* fix: `Label` not taking into account the correct minimum size when using `ellipsis`.
* fix: `Button` horizontal alignment calculations not being correct.
* fix: `Button` not taking into account the correct minimum size when using `ellipsis`.
* fix: `ProgressBar` percentage font positioning to be more centered.
* update: `ProgressBar` to only measure font when value changes.

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
