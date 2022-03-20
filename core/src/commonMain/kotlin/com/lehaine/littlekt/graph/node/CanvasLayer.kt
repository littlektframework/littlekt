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
import com.lehaine.littlekt.util.viewport.ScreenViewport
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

    private var explicitlySet = false

    val canvasCamera = OrthographicCamera()

    val onSizeChanged: Signal = signal()

    var viewport: Viewport = Viewport()
        set(value) {
            field = value
            canvasCamera.viewport = value
            explicitlySet = true
        }

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

    override fun onAddedToScene() {
        super.onAddedToScene()
        if (!explicitlySet) {
            viewport = canvas?.viewport?.let {
                ScreenViewport(it.width, it.height)
            } ?: viewport
        }
        canvasCamera.position.set(canvasCamera.virtualWidth / 2f, canvasCamera.virtualHeight / 2f, 0f)
    }

    override fun resize(width: Int, height: Int) {
        val scene = scene ?: return
        canvasCamera.update(width, height, scene.context)
        canvasCamera.position.set(canvasCamera.viewport.virtualWidth / 2f,
            canvasCamera.viewport.virtualHeight / 2f,
            0f)
        onSizeChanged.emit()
        super.resize(width, height)
    }

    fun render(batch: Batch, renderCallback: ((Node, Batch, Camera) -> Unit)?) {
        val scene = scene ?: return
        if (!enabled) return

        val prevProjMatrix = batch.projectionMatrix
        scene.pushViewport(viewport)
        canvasCamera.update()
        batch.projectionMatrix = canvasCamera.viewProjection
        if (!batch.drawing) batch.begin()
        nodes.forEach {
            it.propagateInternalRender(batch, canvasCamera, renderCallback)
        }
        batch.projectionMatrix = prevProjMatrix
        scene.popViewport()
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
        canvasCamera.screenToWorld(temp, scene.context, temp)
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
        canvasCamera.screenToWorld(vector2, scene?.context ?: error("CanvasLayer is not added to a scene!"), vector2)
        return vector2
    }

    fun canvasToScreenCoordinates(vector2: MutableVec2f): MutableVec2f {
        canvasCamera.worldToScreen(vector2, scene?.context ?: error("CanvasLayer is not added to a scene!"), vector2)
        return vector2
    }
}