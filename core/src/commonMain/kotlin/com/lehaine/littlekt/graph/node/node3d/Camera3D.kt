package com.lehaine.littlekt.graph.node.node3d

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.CanvasLayer
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.math.Vec3f

/**
 * @author Colton Daily
 * @date 12/26/2022
 */
class Camera3D : VisualInstance() {
    var zoom: Float = 1f
    var near: Float = 0.1f
    var far: Float = 100f
    var fov: Float = 60f

    /**
     * Disables other cameras sharing the same [CanvasLayer] or [SceneGraph.root].
     */
    var active: Boolean = false
        set(value) {
            field = value
            if (value) {
                scene?.let {
                    initializeCamera()
                }
            }
        }

    override fun onAddedToScene() {
        super.onAddedToScene()
        if (active) {
            initializeCamera()
        }
    }

    override fun preRender(camera: Camera) {
        if (active) {
            camera.zoom = zoom
            camera.near = near
            camera.far = far
            camera.fov = fov
            camera.position.set(globalCenter.x, globalCenter.y, globalCenter.z)
            camera.update()
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
        error("Unable to find a CanvasLayer or root for $name. Ensure that a Camera2D is a descendant of the scene root or a CanvasLayer.")
    }

    private fun initializeCamera() {
        findClosestCanvas().disableOtherCameras()
    }

    private fun Node.disableOtherCameras() {
        if (this is Camera3D && this != this@Camera3D) {
            if (active) {
                active = false
                return
            }
        }
        nodes.forEach { it.disableOtherCameras() }
    }
}