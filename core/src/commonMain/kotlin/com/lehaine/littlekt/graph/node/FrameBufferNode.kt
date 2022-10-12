package com.lehaine.littlekt.graph.node

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.gl.TexMagFilter
import com.lehaine.littlekt.graphics.gl.TexMinFilter
import com.lehaine.littlekt.graphics.shape.ShapeRenderer
import com.lehaine.littlekt.math.Mat4
import com.lehaine.littlekt.util.SingleSignal
import com.lehaine.littlekt.util.signal1v
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun Node.frameBuffer(
    callback: @SceneGraphDslMarker FrameBufferNode.() -> Unit = {},
): FrameBufferNode {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return FrameBufferNode().also(callback).addTo(this)
}

@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.frameBuffer(
    callback: @SceneGraphDslMarker FrameBufferNode.() -> Unit = {},
): FrameBufferNode {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.frameBuffer(callback)
}

/**
 * A [CanvasLayer] that renders it children to a [FrameBuffer] of a specified size.
 * @author Colton Daily
 * @date 3/14/2022
 */
open class FrameBufferNode : CanvasLayer() {

    /**
     * The color to clear the [FrameBuffer].
     */
    var clearColor: Color = Color.CLEAR

    /**
     * The color buffer texture from the [FrameBuffer].
     */
    val fboTexture: Texture? get() = fbo?.colorBufferTexture

    /**
     * Signal that is emitted when the [FrameBuffer] is resized and recreated.
     */
    val onFboChanged: SingleSignal<Texture> = signal1v()

    private var lastWidth = width
    private var lastHeight = height

    private var fbo: FrameBuffer? = null

    override fun onAddedToScene() {
        super.onAddedToScene()
        if (width != 0 && height != 0) {
            resizeFbo(width, height)
        }
    }

    private fun checkForResize(newWidth: Int, newHeight: Int) {
        if (width != newWidth || height != newHeight) {
            resizeFbo(newWidth, newHeight)
        }
    }

    override fun resize(width: Int, height: Int) {
        // do nothing
    }

    /**
     * Resizes the internal [FrameBuffer] to the new width and height. This will dispose
     * of the previous [FrameBuffer] and the texture.
     * @param newWidth the new width of the [FrameBuffer]
     * @param newHeight the new width of the [FrameBuffer]
     */
    fun resizeFbo(newWidth: Int, newHeight: Int) {
        if (!enabled || isDestroyed) return
        if (newWidth == 0 || newHeight == 0) return
        scene?.let { scene ->
            lastWidth = newWidth
            lastHeight = newHeight
            width = newWidth
            height = newHeight
            fbo?.dispose()
            fbo = FrameBuffer(
                width,
                height,
                minFilter = TexMinFilter.NEAREST,
                magFilter = TexMagFilter.NEAREST
            ).also { it.prepare(scene.context) }
            viewport.width = width
            viewport.height = height
            canvasCamera.ortho(width, height)
            canvasCamera.update()
            fboTexture?.let { onFboChanged.emit(it) }
            onSizeChanged.emit()
        }
    }

    private var prevProjection: Mat4 = Mat4()

    /**
     * Begins drawing to the [FrameBuffer].
     */
    fun begin(batch: Batch) {
        if (!enabled || isDestroyed) return
        checkForResize(lastWidth, lastHeight)
        val fbo = fbo ?: return
        val context = scene?.context ?: return
        val gl = context.gl
        if (width == 0 || height == 0) return
        batch.end()
        prevProjection = batch.projectionMatrix

        canvasCamera.update()
        fbo.begin()
        gl.clearColor(clearColor)
        gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
        batch.begin(canvasCamera.viewProjection)
    }

    override fun propagateInternalRender(
        batch: Batch,
        camera: Camera,
        shapeRenderer: ShapeRenderer,
        renderCallback: ((Node, Batch, Camera, ShapeRenderer) -> Unit)?,
    ) {
        if (!enabled || isDestroyed) return
        fbo ?: return
        if (width == 0 || height == 0) return
        begin(batch)
        nodes.forEach { it.propagateInternalRender(batch, canvasCamera, shapeRenderer, renderCallback) }
        end(batch)
    }

    /**
     * Finishes drawing to the [FrameBuffer].
     */
    fun end(batch: Batch) {
        if (!enabled || isDestroyed) return
        val fbo = fbo ?: return
        if (width == 0 || height == 0) return
        batch.end()
        fbo.end()
        batch.begin(prevProjection)
    }

    override fun onDestroy() {
        super.onDestroy()
        onFboChanged.clear()
    }
}