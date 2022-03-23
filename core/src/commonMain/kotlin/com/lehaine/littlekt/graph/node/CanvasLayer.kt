package com.lehaine.littlekt.graph.node

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graph.node.ui.Control
import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.OrthographicCamera
import com.lehaine.littlekt.math.MutableVec2f
import com.lehaine.littlekt.util.Signal
import com.lehaine.littlekt.util.signal
import com.lehaine.littlekt.util.viewport.Viewport
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Adds a [CanvasLayer] to the current [Node] as a child and then triggers the [callback]
 * @param callback the callback that is invoked with a [CanvasLayer] context in order to initialize any values
 * @return the newly created [CanvasLayer]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.canvasLayer(callback: @SceneGraphDslMarker CanvasLayer.() -> Unit = {}): CanvasLayer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return CanvasLayer().also(callback).addTo(this)
}

/**
 * Adds a [CanvasLayer] to the current [SceneGraph.root] as a child and then triggers the [CanvasLayer]
 * @param callback the callback that is invoked with a [CanvasLayer] context in order to initialize any values
 * @return the newly created [CanvasLayer]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.canvasLayer(callback: @SceneGraphDslMarker CanvasLayer.() -> Unit = {}): CanvasLayer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.canvasLayer(callback)
}

/**
 * A [Node] that uses a separate [OrthographicCamera] for rendering instead of the inherited camera from the [SceneGraph].
 * This is useful to render a list of nodes with a camera a certain way and another list of nodes another way.
 *
 * For example: When rendering certain nodes at a low resolution using a [Viewport] to render UI at a higher resolution.
 *
 * @author Colton Daily
 * @date 3/13/2022
 */
open class CanvasLayer : Node() {

    var viewport: Viewport = Viewport()
    val canvasCamera: OrthographicCamera get() = viewport.camera as OrthographicCamera

    val onSizeChanged: Signal = signal()

    var virtualWidth: Float
        get() = viewport.virtualWidth
        set(value) {
            viewport.virtualWidth = value
        }

    var virtualHeight: Float
        get() = viewport.virtualHeight
        set(value) {
            viewport.virtualHeight = value
        }

    var width: Int
        get() = viewport.width
        set(value) {
            viewport.width = value
        }

    var height: Int
        get() = viewport.height
        set(value) {
            viewport.height = value
        }

    var x: Int
        get() = viewport.x
        set(value) {
            viewport.x = value
        }

    var y: Int
        get() = viewport.y
        set(value) {
            viewport.y = value
        }

    private val temp = MutableVec2f()

    override fun resize(width: Int, height: Int) {
        canvasCamera.ortho(width, height)
        onSizeChanged.emit()

        super.resize(width, height)
    }

    open fun render(batch: Batch, renderCallback: ((Node, Batch, Camera) -> Unit)?) {
        if (!enabled) return
        val scene = scene ?: return

        val prevProjMatrix = batch.projectionMatrix
        canvasCamera.ortho(scene.context.graphics.width, scene.context.graphics.height)
        canvasCamera.update()
        batch.projectionMatrix = canvasCamera.viewProjection
        if (!batch.drawing) batch.begin()
        nodes.forEach {
            it.propagateInternalRender(batch, canvasCamera, renderCallback)
        }
        batch.projectionMatrix = prevProjMatrix
    }

    override fun propagateInternalRender(
        batch: Batch,
        camera: Camera,
        renderCallback: ((Node, Batch, Camera) -> Unit)?,
    ) {
        render(batch, renderCallback)
    }

    override fun propagateHit(hx: Float, hy: Float): Control? {
        val scene = scene ?: return null
        scene.sceneToScreenCoordinates(temp.set(hx, hy))
        canvasCamera.screenToWorld(scene.context, temp, viewport, temp)
        nodes.forEachReversed {
            val target = it.propagateHit(temp.x, temp.y)
            if (target != null) {
                return target
            }
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        onSizeChanged.clear()
    }

    fun screenToCanvasCoordinates(vector2: MutableVec2f): MutableVec2f {
        canvasCamera.screenToWorld(scene?.context ?: error("CanvasLayer is not added to a scene!"),
            vector2,
            viewport,
            vector2)
        return vector2
    }

    fun canvasToScreenCoordinates(vector2: MutableVec2f): MutableVec2f {
        canvasCamera.worldToScreen(scene?.context ?: error("CanvasLayer is not added to a scene!"),
            vector2,
            viewport,
            vector2)
        return vector2
    }
}