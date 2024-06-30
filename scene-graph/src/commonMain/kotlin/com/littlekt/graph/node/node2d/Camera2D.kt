package com.littlekt.graph.node.node2d

import com.littlekt.graph.SceneGraph
import com.littlekt.graph.node.CanvasLayer
import com.littlekt.graph.node.Node
import com.littlekt.graph.node.addTo
import com.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.littlekt.graphics.Camera
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.roundToInt

/**
 * Adds a [Camera2D] to the current [Node] as a child and then triggers the [callback]
 *
 * @param callback the callback that is invoked with a [Camera2D] context in order to initialize any
 *   values
 * @return the newly created [Camera2D]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.camera2d(callback: @SceneGraphDslMarker Camera2D.() -> Unit = {}): Camera2D {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return Camera2D().also(callback).addTo(this)
}

/**
 * Adds a [Camera2D] to the current [SceneGraph.root] as a child and then triggers the [Camera2D]
 *
 * @param callback the callback that is invoked with a [Camera2D] context in order to initialize any
 *   values
 * @return the newly created [Camera2D]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.camera2d(
    callback: @SceneGraphDslMarker Camera2D.() -> Unit = {}
): Camera2D {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.camera2d(callback)
}

/**
 * A [Node2D] that scrolls the closest rendering [Camera]. The rendering [Camera] will either be
 * from a [CanvasLayer] or the [SceneGraph]. Only one [Camera2D] can be active at a time per
 * [Camera].
 *
 * @author Colton Daily
 * @date 3/13/2022
 */
open class Camera2D : Node2D() {

    var snapToPixel = false
    var zoom: Float = 1f
    var near: Float = 0f
    var far: Float = 1000f

    /** Disables other cameras sharing the same [CanvasLayer] or [SceneGraph.root]. */
    var active: Boolean = false
        set(value) {
            field = value
            if (value) {
                scene?.let { initializeCamera() }
            }
        }

    override fun onAddedToScene() {
        super.onAddedToScene()
        if (active) {
            initializeCamera()
        }
    }

    private fun Node.findClosestCanvas(): Node {
        var current: Node? = this
        while (current != null) {
            val parent = current.parent
            if (parent is CanvasLayer || parent != null && parent == scene?.root) {
                return parent
            } else {
                current = parent
            }
        }
        error(
            "Unable to find a CanvasLayer or root for $name. Ensure that a Camera2D is a descendant of the scene root or a CanvasLayer."
        )
    }

    private fun initializeCamera() {
        findClosestCanvas().disableOtherCameras()
        canvas?.canvasCamera?.updateCameraWithLatest()
    }

    override fun onPositionChanged() {
        super.onPositionChanged()
        canvas?.canvasCamera?.updateCameraWithLatest()
    }

    override fun preRender(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
        camera.updateCameraWithLatest()
    }

    private fun Camera.updateCameraWithLatest() {
        if (active) {
            this.zoom = this@Camera2D.zoom
            this.near = this@Camera2D.near
            this.far = this@Camera2D.far
            if (snapToPixel) {
                position.set(globalX.roundToInt().toFloat(), globalY.roundToInt().toFloat(), 0f)
            } else {
                position.set(globalX, globalY, 0f)
            }
        }
    }

    private fun Node.disableOtherCameras() {
        if (this is Camera2D && this != this@Camera2D) {
            if (active) {
                active = false
                return
            }
        }
        nodes.forEach { it.disableOtherCameras() }
    }
}
