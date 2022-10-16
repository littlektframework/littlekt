package com.lehaine.littlekt.graph.node

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.shape.ShapeRenderer
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Adds a [ViewportCanvasLayer] to the current [Node] as a child and then triggers the [callback]
 * @param callback the callback that is invoked with a [ViewportCanvasLayer] context in order to initialize any values
 * @return the newly created [ViewportCanvasLayer]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.viewport(callback: @SceneGraphDslMarker ViewportCanvasLayer.() -> Unit = {}): ViewportCanvasLayer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return ViewportCanvasLayer().also(callback).addTo(this)
}

/**
 * Adds a [ViewportCanvasLayer] to the current [SceneGraph.root] as a child and then triggers the [ViewportCanvasLayer]
 * @param callback the callback that is invoked with a [ViewportCanvasLayer] context in order to initialize any values
 * @return the newly created [ViewportCanvasLayer]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.viewport(callback: @SceneGraphDslMarker ViewportCanvasLayer.() -> Unit = {}): ViewportCanvasLayer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.viewport(callback)
}

/**
 * A [CanvasLayer] that handles updating the [viewport] and uses the [viewport] to render its children.
 * @author Colton Daily
 * @date 3/22/2022
 */
open class ViewportCanvasLayer : CanvasLayer() {

    override fun resize(width: Int, height: Int) {
        val scene = scene ?: return
        viewport.update(width, height, scene.context, true)
        onSizeChanged.emit()
    }

    override fun render(
        batch: Batch,
        shapeRenderer: ShapeRenderer,
        renderCallback: ((Node, Batch, Camera, ShapeRenderer) -> Unit)?,
    ) {
        val scene = scene ?: return
        if (!enabled || isDestroyed) return

        val prevProjMatrix = batch.projectionMatrix
        scene.pushViewport(viewport)
        canvasCamera.update()
        batch.projectionMatrix = canvasCamera.viewProjection
        if (!batch.drawing) batch.begin()
        nodes.forEach {
            it.propagateInternalRender(batch, canvasCamera, shapeRenderer, renderCallback)
            if (scene.showDebugInfo) it.propagateInternalDebugRender(batch, canvasCamera, shapeRenderer, renderCallback)
        }
        batch.projectionMatrix = prevProjMatrix
        scene.popViewport()
    }
}