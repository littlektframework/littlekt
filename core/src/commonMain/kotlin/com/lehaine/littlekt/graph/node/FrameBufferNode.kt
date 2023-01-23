package com.lehaine.littlekt.graph.node

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graph.node.resource.InputEvent
import com.lehaine.littlekt.graph.node.ui.Control
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.g2d.Batch
import com.lehaine.littlekt.graphics.g2d.shape.ShapeRenderer
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.gl.TexMagFilter
import com.lehaine.littlekt.graphics.gl.TexMinFilter
import com.lehaine.littlekt.math.Mat4
import com.lehaine.littlekt.math.MutableVec2f
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

    private val tempVec = MutableVec2f()

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
            canvasCamera3d.virtualWidth = width.toFloat()
            canvasCamera3d.virtualHeight = height.toFloat()
            canvasCamera.ortho(width, height)
            canvasCamera.update()
            canvasCamera3d.update()
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
        if (batch.drawing) batch.end()
        prevProjection = batch.projectionMatrix

        canvasCamera.update()
        canvasCamera3d.update()
        fbo.begin()
        gl.clearColor(clearColor)
        gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
        batch.projectionMatrix = canvasCamera.viewProjection
    }

    override fun propagateInternalRender(
        batch: Batch,
        camera: Camera,
        camera3d: Camera,
        shapeRenderer: ShapeRenderer,
        renderCallback: ((Node, Batch, Camera, Camera, ShapeRenderer) -> Unit)?,
    ) {
        if (!enabled || isDestroyed) return
        fbo ?: return
        if (width == 0 || height == 0) return
        val scene = scene ?: return
        begin(batch)
        nodes.forEach {
            it.propagateInternalRender(batch, canvasCamera, canvasCamera3d, shapeRenderer, renderCallback)
            if (scene.showDebugInfo) it.propagateInternalDebugRender(
                batch,
                camera,
                camera3d,
                shapeRenderer,
                renderCallback
            )
        }
        end(batch)
    }

    override fun propagateHit(hx: Float, hy: Float): Control? {
        scene ?: return null
        if (!enabled || isDestroyed) return null
        tempVec.set(hx - width * 0.5f + canvasCamera.position.x, hy - height * 0.5f + canvasCamera.position.y)
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

    /**
     * Finishes drawing to the [FrameBuffer].
     */
    fun end(batch: Batch) {
        if (!enabled || isDestroyed) return
        val fbo = fbo ?: return
        if (width == 0 || height == 0) return
        if (batch.drawing) batch.end()
        fbo.end()
        batch.projectionMatrix = prevProjection
    }


    override fun onDestroy() {
        super.onDestroy()
        onFboChanged.clear()
    }
}