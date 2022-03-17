package com.lehaine.littlekt.graph.node

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.OrthographicCamera
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
 * @author Colton Daily
 * @date 3/13/2022
 */
open class CanvasLayer : Node() {

    private val canvasCamera = OrthographicCamera()

    override fun onAddedToScene() {
        super.onAddedToScene()
        canvasCamera.viewport = viewport ?: error("Unable to set CanvasLayer transform viewport")
        canvasCamera.position.set(canvasCamera.virtualWidth / 2f, canvasCamera.virtualHeight / 2f, 0f)
    }

    override fun _onResize(width: Int, height: Int, center: Boolean) {
        val context = scene?.context ?: return
        canvasCamera.update(width, height, context)
        if (center) {
            canvasCamera.position.set(canvasCamera.viewport.virtualWidth / 2f,
                canvasCamera.viewport.virtualHeight / 2f,
                0f)
        }
        super._onResize(width, height, center)
    }

    override fun render(batch: Batch, camera: Camera, renderCallback: ((Node, Batch, Camera) -> Unit)?) {
        if (!enabled) return
        val prevProjMatrix = batch.projectionMatrix
        canvasCamera.viewport = viewport ?: error("Unable to set CanvasLayer transform viewport")
        canvasCamera.update()
        batch.projectionMatrix = canvasCamera.viewProjection
        nodes.forEach {
            it.render(batch, canvasCamera, renderCallback)
        }
        batch.projectionMatrix = prevProjMatrix
    }
}