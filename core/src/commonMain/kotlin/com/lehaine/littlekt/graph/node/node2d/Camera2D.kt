package com.lehaine.littlekt.graph.node.node2d

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.CanvasLayer
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graphics.OrthographicCamera
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.Duration

/**
 * Adds a [Camera2D] to the current [Node] as a child and then triggers the [callback]
 * @param callback the callback that is invoked with a [Camera2D] context in order to initialize any values
 * @return the newly created [Camera2D]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.camera2d(callback: @SceneGraphDslMarker Camera2D.() -> Unit = {}): Camera2D {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return Camera2D().also(callback).addTo(this)
}

/**
 * Adds a [Camera2D] to the current [SceneGraph.root] as a child and then triggers the [Camera2D]
 * @param callback the callback that is invoked with a [Camera2D] context in order to initialize any values
 * @return the newly created [Camera2D]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.camera2d(callback: @SceneGraphDslMarker Camera2D.() -> Unit = {}): Camera2D {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.camera2d(callback)
}

/**
 * @author Colton Daily
 * @date 3/13/2022
 */
class Camera2D : Node2D() {

    private var camera: OrthographicCamera? = null

    var active: Boolean = false
        set(value) {
            field = value
            if (value) {
                scene?.let {
                    println("added")
                    it.root.disableOtherCameras()
                    findClosestCanvasTransform()
                    onPositionChanged()
                }
            }
        }

    override fun onAddedToScene() {
        super.onAddedToScene()
        if (active) {
            findClosestCanvasTransform()
            camera?.position?.set(globalX, globalY, 0f)
        }
    }

    override fun update(dt: Duration) {
        if (active) {
            camera?.position?.set(globalX, globalY, 0f)
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

    private fun findClosestCanvasTransform() {
        var current: Node? = this
        while (current != null) {
            when (val parent = current.parent) {
                is CanvasLayer -> {
                    camera = parent.camera
                    return
                }
                scene?.root -> {
                    camera = scene?.camera ?: error("Unable to set to camera root")
                    return
                }
                else -> {
                    current = parent
                }
            }
        }
    }
}