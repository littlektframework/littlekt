package com.littlekt.graph.node

import com.littlekt.graph.SceneGraph
import com.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.littlekt.graph.node.resource.InputEvent
import com.littlekt.graph.node.ui.CanvasLayerContainer
import com.littlekt.graph.node.ui.Control
import com.littlekt.graph.util.Signal
import com.littlekt.graph.util.signal
import com.littlekt.graphics.*
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.SpriteBatchShader
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.webgpu.*
import com.littlekt.log.Logger
import com.littlekt.math.Mat4
import com.littlekt.math.MutableVec2f
import com.littlekt.math.MutableVec3f
import com.littlekt.util.viewport.Viewport
import com.littlekt.util.viewport.setViewport
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Adds a [CanvasLayer] to the current [Node] as a child and then triggers the [callback].
 *
 * @param callback the callback that is invoked with a [CanvasLayer] context in order to initialize
 *   any values
 * @return the newly created [CanvasLayer]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.canvasLayer(
    callback: @SceneGraphDslMarker CanvasLayer.() -> Unit = {}
): CanvasLayer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return CanvasLayer().also(callback).addTo(this)
}

/**
 * Adds a [CanvasLayer] to the current [SceneGraph.root] as a child and then triggers the
 * [CanvasLayer]
 *
 * @param callback the callback that is invoked with a [CanvasLayer] context in order to initialize
 *   any values
 * @return the newly created [CanvasLayer]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.canvasLayer(
    callback: @SceneGraphDslMarker CanvasLayer.() -> Unit = {}
): CanvasLayer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.canvasLayer(callback)
}

/**
 * A [Node] that contains a [Viewport], [OrthographicCamera], and [PerspectiveCamera]. The
 * [CanvasLayer] expects a texture to draw to. By default, if a texture isn't specified via
 * [CanvasLayer.canvasRenderPassDescriptor], then a new [EmptyTexture] is created internally and any
 * rendering will be outputted to it. This node alone will not display any rendering. The
 * [CanvasLayer.target] must be used to draw the result.
 *
 * @see CanvasLayerContainer
 * @author Colton Daily
 * @date 3/13/2022
 */
open class CanvasLayer : Node() {

    /**
     * Viewport instance that can be used for rendering children nodes in inherited classes. This is
     * not used directly in the base [CanvasLayer] class.
     *
     * @see CanvasLayerContainer
     */
    var viewport: Viewport = Viewport()

    /** The [OrthographicCamera] of this [CanvasLayer]. This may be manipulated. */
    val canvasCamera: OrthographicCamera
        get() = viewport.camera as OrthographicCamera

    /** The [PerspectiveCamera] of this [CanvasLayer]. This may be manipulated. */
    val canvasCamera3d: PerspectiveCamera = PerspectiveCamera() // TODO refactor use 3d viewport

    /** Signal that is emitted when the viewport dimensions are changed by the [CanvasLayer]. */
    val onSizeChanged: Signal = signal()

    /** The viewport virtual/world width */
    var virtualWidth: Float
        get() = viewport.virtualWidth
        set(value) {
            viewport.virtualWidth = value
        }

    /** The viewport virtual/world height */
    var virtualHeight: Float
        get() = viewport.virtualHeight
        set(value) {
            viewport.virtualHeight = value
        }

    /** Width of the viewport */
    var width: Int
        get() = viewport.width
        set(value) {
            viewport.width = value
        }

    /** Height of the viewport */
    var height: Int
        get() = viewport.height
        set(value) {
            viewport.height = value
        }

    /** Viewport x-coord */
    var x: Int
        get() = viewport.x
        set(value) {
            viewport.x = value
        }

    /** Viewport y-coord */
    var y: Int
        get() = viewport.y
        set(value) {
            viewport.y = value
        }

    /**
     * The current [RenderPassEncoder] for this [CanvasLayer]. Will throw an exception if
     * [renderPassOrNull] is `null`.
     *
     * @see pushRenderPass
     * @see popAndEndRenderPass
     * @see renderPassOrNull
     */
    val renderPass: RenderPassEncoder
        get() = renderPassOrNull ?: error("This CanvasLayer RenderPass has not be set!")

    /**
     * The current [RenderPassEncoder] for this [CanvasLayer]. This is the backing field for
     * [renderPass].
     *
     * @see pushRenderPass
     * @see popAndEndRenderPass
     * @see renderPass
     */
    var renderPassOrNull: RenderPassEncoder? = null
        private set

    /**
     * This [CanvasLayer] render pass descriptor to be used in [renderPassDescriptor]. Defaults to
     * `null`.
     */
    var canvasRenderPassDescriptor: RenderPassDescriptor? = null

    /**
     * if `true`, then the underlying render target will be resized automatically based on [resize].
     * If set to `false`, then this will not resize at all.
     */
    var resizeAutomatically: Boolean = true
    /**
     * The [RenderPassDescriptor] to be used when beginning a new render pass for the current
     * [CanvasLayer]. This will first check [canvasRenderPassDescriptor], if set, if not then it
     * will pull from this node's: `canvas.renderPassDescriptor`.
     */
    val renderPassDescriptor: RenderPassDescriptor
        get() =
            canvasRenderPassDescriptor
                ?: canvas?.renderPassDescriptor
                ?: error(
                    "A RenderPassDescriptor cannot be found for this CanvasLayer! Either set it via canvasRenderPassDescriptor or add it to a CanvasLayer that has one."
                )

    protected val renderPasses = mutableListOf<RenderPassEncoder>()

    private var _spriteShader: Shader? = null

    /** The [Logger] for this [CanvasLayer]. */
    protected val logger by lazy { Logger("CanvasLayer($name)") }

    /**
     * The [Shader] that will be passed into the [Batch.shader] before the [CanvasLayer] propagates
     * the render calls. This required to do so in case the [canvasCamera] is mutated and there are
     * multiple [CanvasLayer] used in the graph. This prevents overwriting the previous
     * [canvasCamera] view-projection matrix data. This defaults to [SpriteBatchShader].
     */
    var spriteShader: Shader
        get() {
            if (_spriteShader == null) {
                _spriteShader = SpriteBatchShader(context.graphics.device)
            }
            return _spriteShader ?: error("Unable to get initial Shader value")
        }
        set(value) {
            _spriteShader = value
        }

    /** The color to clear the canvas layer. */
    var clearColor: Color = Color.CLEAR

    /**
     * The output render target of this [CanvasLayer]. This can be used in order to draw the
     * results.
     */
    val target: Texture
        get() = fbo

    private var lastWidth = width
    private var lastHeight = height

    private val fbo: EmptyTexture by lazy {
        EmptyTexture(context.graphics.device, context.graphics.preferredFormat, 1, 1)
    }

    private val tempVec = MutableVec2f()
    private var prevProjection: Mat4 = Mat4()
    private var prevShader: Shader? = null

    override fun onAddedToScene() {
        super.onAddedToScene()
        if (!resizeAutomatically) return
        if (width != 0 && height != 0) {
            resizeFbo(width, height)
        }
    }

    private fun checkForResize(newWidth: Int, newHeight: Int) {
        if (!resizeAutomatically) return
        if (width != newWidth || height != newHeight) {
            resizeFbo(newWidth, newHeight)
        }
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
        canvasCamera3d.virtualWidth = width.toFloat()
        canvasCamera3d.virtualHeight = height.toFloat()
        canvasCamera.ortho(width, height)
        viewport.width = width
        viewport.height = height
        onSizeChanged.emit()

        super.resize(width, height)
    }

    /**
     * Resizes the internal [EmptyTexture] to the new width and height. This will dispose of the
     * previous [EmptyTexture] and the texture. This is not needed if [resizeAutomatically] is set
     * to `false`.
     *
     * @param newWidth the new width of the [EmptyTexture]
     * @param newHeight the new width of the [EmptyTexture]
     */
    fun resizeFbo(newWidth: Int, newHeight: Int) {
        if (!enabled || isDestroyed) return
        if (newWidth == 0 || newHeight == 0) return

        lastWidth = newWidth
        lastHeight = newHeight
        width = newWidth
        height = newHeight

        fbo.resize(width, height)
        canvasRenderPassDescriptor =
            RenderPassDescriptor(
                colorAttachments =
                    listOf(
                        RenderPassColorAttachmentDescriptor(
                            view = fbo.view,
                            loadOp = LoadOp.CLEAR,
                            storeOp = StoreOp.STORE,
                            clearColor = clearColor
                        )
                    ),
                label = "Canvas Layer Pass"
            )

        resize(width, height)
    }

    /** Begins drawing to the [EmptyTexture]. */
    private fun begin(batch: Batch) {
        if (!enabled || isDestroyed) return
        checkForResize(lastWidth, lastHeight)
        if (width == 0 || height == 0) return
        prevProjection = batch.viewProjection
        prevShader = batch.shader
        val canvasRenderPass = canvasRenderPassOrNull
        if (canvasRenderPass != null && batch.drawing) {
            batch.flush(canvasRenderPass)
        }
        batch.shader = spriteShader
        canvasCamera.update()
        canvasCamera3d.update()
        batch.viewProjection = canvasCamera.viewProjection
        canvas?.let { popAndEndCanvasRenderPass() }
        pushRenderPass(renderPassDescriptor.label, renderPassDescriptor)
    }

    open fun render(
        batch: Batch,
        shapeRenderer: ShapeRenderer,
        renderCallback: ((Node, Batch, Camera, Camera, ShapeRenderer) -> Unit)?,
    ) {
        if (!enabled || isDestroyed) return
        if (width == 0 || height == 0) return
        val scene = scene ?: return
        begin(batch)
        nodes.forEach {
            it.propagateInternalRender(
                batch = batch,
                camera = canvasCamera,
                camera3d = canvasCamera3d,
                shapeRenderer = shapeRenderer,
                renderCallback = renderCallback
            )
            if (scene.showDebugInfo) {
                it.propagateInternalDebugRender(
                    batch = batch,
                    camera = canvasCamera,
                    camera3d = canvasCamera3d,
                    shapeRenderer = shapeRenderer,
                    renderCallback = renderCallback
                )
            }
        }
        end(batch)
    }

    override fun propagateInternalRender(
        batch: Batch,
        camera: Camera,
        camera3d: Camera,
        shapeRenderer: ShapeRenderer,
        renderCallback: ((Node, Batch, Camera, Camera, ShapeRenderer) -> Unit)?,
    ) {
        render(batch, shapeRenderer, renderCallback)
    }

    override fun propagateInternalDebugRender(
        batch: Batch,
        camera: Camera,
        camera3d: Camera,
        shapeRenderer: ShapeRenderer,
        renderCallback: ((Node, Batch, Camera, Camera, ShapeRenderer) -> Unit)?,
    ) {
        // we override this and make it do nothing so that we don't make multiple calls
        // to debugRender with nested CanvasLayers.
    }

    override fun propagateHit(hx: Float, hy: Float): Control? {
        scene ?: return null
        if (!enabled || isDestroyed) return null
        tempVec.set(
            hx - width * 0.5f + canvasCamera.position.x,
            hy - height * 0.5f + canvasCamera.position.y
        )
        // we don't need to convert to canvas coords because the FrameBufferContainer handles
        // all of that. We just need to pass it down
        nodes.forEachReversed {
            val target = it.propagateHit(tempVec.x, tempVec.y)
            if (target != null) {
                return target
            }
        }
        return null
    }

    override fun propagateInput(event: InputEvent<*>): Boolean {
        scene ?: return false
        if (!enabled || isDestroyed) return false
        tempVec.set(
            event.canvasX - width * 0.5f + canvasCamera.position.x,
            event.canvasY - height * 0.5f + canvasCamera.position.y
        )
        nodes.forEachReversed {
            // we set canvas coords every iteration just in case a child CanvasLayer changes it
            event.canvasX = tempVec.x
            event.canvasY = tempVec.y
            it.propagateInput(event)
            if (event.handled) {
                return true
            }
        }
        callInput(event)
        return event.handled
    }

    override fun propagateUnhandledInput(event: InputEvent<*>): Boolean {
        scene ?: return false
        if (!enabled || isDestroyed) return false
        tempVec.set(
            event.canvasX - width * 0.5f + canvasCamera.position.x,
            event.canvasY - height * 0.5f + canvasCamera.position.y
        )
        nodes.forEachReversed {
            // we set canvas coords every iteration just in case a child CanvasLayer changes it
            event.canvasX = tempVec.x
            event.canvasY = tempVec.y
            it.propagateUnhandledInput(event)
            if (event.handled) {
                return true
            }
        }
        callUnhandledInput(event)
        return event.handled
    }

    /** Finishes drawing to the [EmptyTexture]. */
    private fun end(batch: Batch) {
        if (!enabled || isDestroyed) return
        if (width == 0 || height == 0) return
        if (batch.drawing) {
            batch.flush(renderPass)
        }
        batch.viewProjection = prevProjection
        batch.shader =
            prevShader ?: error("Unable to set Batch.shader back to its previous shader!")
        popAndEndRenderPass()
        canvas?.let { pushRenderPassToCanvas("${canvas?.name} pass") }
        if (renderPasses.isNotEmpty()) {
            logger.warn {
                "This CanvasLayer($name) flushed with '${renderPasses.size}' pending render passes. Ensure any pushed render passes are popped!"
            }
            renderPasses.forEach {
                it.end()
                it.release()
            }
        }
        renderPasses.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        onSizeChanged.clear()
        fbo.release()
        _spriteShader?.release()
        renderPasses.forEach { it.release() }
    }

    /**
     * Calls [RenderPassEncoder.setScissorRect] directly. This ensures the given scissor rect is
     * within the renter target bounds.
     */
    open fun setScissorRect(x: Int, y: Int, width: Int, height: Int) {
        // we need to flip it since frame buffers start at 0,0 on the top left
        @Suppress("NAME_SHADOWING") val y = this.height - y - height
        val x2 = x + width
        val y2 = y + height
        if (x2 <= 0 || x > this.width || y2 <= 0 || y > this.height) {
            // outside of bounds completely, so lets just set the scissor to nothing
            renderPass.setScissorRect(0, 0, 0, 0)
            return
        }
        val scissorX = if (x < 0) 0 else x
        val scissorY = if (y < 0) 0 else y
        val scissorWidth = if (x2 < this.width) x2 - scissorX else this.width - scissorX
        val scissorHeight = if (y2 < this.height) y2 - scissorY else this.height - scissorY

        renderPass.setScissorRect(scissorX, scissorY, scissorWidth, scissorHeight)
    }

    /**
     * Begins a new [RenderPassEncoder]. Any subsequent [CanvasItem.render] will use the specified
     * render pass. Once finished, call [popAndEndRenderPass] to remove and set it back to the
     * previous render pass. This assumes multiple render passes in a single command pass. Hence,
     * the default descriptor will automatically default the previous render passes
     * [RenderPassDescriptor] while also setting the color attachments
     * [RenderPassColorAttachmentDescriptor.loadOp] to [LoadOp.LOAD]. You are responsible for
     * calling [popAndEndRenderPass] with this new render pass, as this a helper method to create
     * one.
     *
     * @param label a label for the render pass
     * @param descriptor the default descriptor assumes the color attachments to use [LoadOp.LOAD].
     * @return the newly created [RenderPassEncoder].
     */
    fun pushRenderPass(
        label: String? = null,
        descriptor: RenderPassDescriptor = run {
            val initDesc = renderPassDescriptor
            val colorAttachments = initDesc.colorAttachments.map { it.copy(loadOp = LoadOp.LOAD) }
            initDesc.copy(colorAttachments = colorAttachments, label = label)
        }
    ) {
        val scene = scene ?: error("CanvasLayer is not part of a scene!")
        val result =
            scene.commandEncoder?.beginRenderPass(descriptor)
                ?: error("Command encoder has not been set on the graph!")
        renderPasses += result
        renderPassOrNull = result
        result.setViewport(viewport)
    }

    /**
     * Removes the last [RenderPassEncoder] that was last added with [pushRenderPassToCanvas] and
     * ends and releases it via [RenderPassEncoder.end] and [RenderPassEncoder.release]. If any
     * render passes are left in the list, [renderPass] will be set to it.
     */
    fun popAndEndRenderPass() {
        if (renderPasses.isNotEmpty()) {
            val removed = renderPasses.removeLast()
            removed.end()
            removed.release()
            renderPassOrNull = null
            if (renderPasses.isNotEmpty()) {
                renderPassOrNull = renderPasses.last()
            }
        }
    }

    /**
     * Convert screen coordinates to local canvas coordinates.
     *
     * @param vector2 the input screen coordinates. This is also used as the `out` vector.
     */
    fun screenToCanvasCoordinates(vector2: MutableVec2f): MutableVec2f {
        canvasCamera.screenToWorld(
            scene?.context ?: error("CanvasLayer is not added to a scene!"),
            vector2,
            viewport,
            vector2
        )
        return vector2
    }

    /**
     * Convert screen coordinates to local canvas coordinates.
     *
     * @param vector3 the input screen coordinates. This is also used as the `out` vector.
     */
    fun screenToCanvasCoordinates(vector3: MutableVec3f): MutableVec3f {
        canvasCamera.screenToWorld(
            scene?.context ?: error("CanvasLayer is not added to a scene!"),
            vector3,
            viewport,
            vector3
        )
        return vector3
    }

    /**
     * Convert canvas coordinates to screen coordinates.
     *
     * @param vector2 the input canvas coordinates. This is also used as the `out` vector.
     */
    fun canvasToScreenCoordinates(vector2: MutableVec2f): MutableVec2f {
        canvasCamera.worldToScreen(
            scene?.context ?: error("CanvasLayer is not added to a scene!"),
            vector2,
            viewport,
            vector2
        )
        return vector2
    }

    /**
     * Convert canvas coordinates to screen coordinates.
     *
     * @param vector3 the input canvas coordinates. This is also used as the `out` vector.
     */
    fun canvasToScreenCoordinates(vector3: MutableVec3f): MutableVec3f {
        canvasCamera.worldToScreen(
            scene?.context ?: error("CanvasLayer is not added to a scene!"),
            vector3,
            viewport,
            vector3
        )
        return vector3
    }
}
