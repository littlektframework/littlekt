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
 * A [Node] that uses a separate [OrthographicCamera] for rendering instead of the inherited camera from the [SceneGraph].
 * This is useful to render a list of nodes with a camera a certain way and another list of nodes another way.
 *
 * For example: When rendering certain nodes at a low resolution using a [CanvasLayer] to render UI at a higher resolution.
 *
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

    override fun resize(width: Int, height: Int) {
        val context = scene?.context ?: return
        canvasCamera.update(width, height, context)
        canvasCamera.position.set(canvasCamera.viewport.virtualWidth / 2f,
            canvasCamera.viewport.virtualHeight / 2f,
            0f)
        super.resize(width, height)
    }

    override fun propagateInternalRender(
        batch: Batch,
        camera: Camera,
        renderCallback: ((Node, Batch, Camera) -> Unit)?,
    ) {
        if (!enabled) return
        val prevProjMatrix = batch.projectionMatrix
        canvasCamera.viewport = viewport ?: error("Unable to set CanvasLayer transform viewport")
        canvasCamera.update()
        batch.projectionMatrix = canvasCamera.viewProjection
        nodes.forEach {
            it.propagateInternalRender(batch, canvasCamera, renderCallback)
        }
        batch.projectionMatrix = prevProjMatrix
    }
}