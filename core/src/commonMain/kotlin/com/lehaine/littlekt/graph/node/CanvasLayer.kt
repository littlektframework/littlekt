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

    internal val camera = OrthographicCamera()

    override fun onAddedToScene() {
        super.onAddedToScene()
        camera.viewport = viewport ?: error("Unable to set CanvasLayer transform viewport")
        camera.position.set(camera.viewport.virtualWidth / 2f, camera.viewport.virtualHeight / 2f, 0f)
    }

    override fun _onResize(width: Int, height: Int, center: Boolean) {
        camera.update()
        if (center) {
            camera.position.set(camera.viewport.virtualWidth / 2f, camera.viewport.virtualHeight / 2f, 0f)
        }
        super._onResize(width, height, center)
    }

    override fun _render(batch: Batch, camera: Camera, renderCallback: ((Node, Batch, Camera) -> Unit)?) {
        if (!enabled || !visible) return
        val prevProjMatrix = batch.projectionMatrix
        scene?.let {
            this.camera.viewport = viewport ?: error("Unable to set CanvasLayer transform viewport")
            this.camera.update()
        }
        batch.projectionMatrix = this.camera.viewProjection
        renderCallback?.invoke(this, batch, this.camera)
        render(batch, this.camera)
        onRender.emit(batch, this.camera)
        nodes.forEach {
            it._render(batch, this.camera, renderCallback)
        }
        batch.projectionMatrix = prevProjMatrix
    }


}