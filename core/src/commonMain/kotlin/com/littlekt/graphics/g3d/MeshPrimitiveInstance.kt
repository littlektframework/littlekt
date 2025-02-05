package com.littlekt.graphics.g3d

import com.littlekt.graphics.Camera
import com.littlekt.graphics.Color
import com.littlekt.math.spatial.BoundingBox
import com.littlekt.util.datastructure.fastForEach

/**
 * @author Colton Daily
 * @date 1/15/2025
 */
open class MeshPrimitiveInstance : Node3D() {
    var color: Color = Color.WHITE
        set(value) {
            field = value
            dirty()
        }

    override var frustumCulled: Boolean = true

    /** Don't set this */
    var instanceOf: MeshPrimitive? = null

    override fun dirty() {
        super.dirty()
        instanceOf?.instanceDirty(this)
    }

    fun addTo(meshPrimitive: MeshPrimitive) {
        meshPrimitive.addInstance(this)
    }

    fun remove() {
        instanceOf?.removeInstance(this)
    }

    override fun color(color: Color) {
        this.color = color
        super.color(color)
    }

    override fun addToBoundingBox(bounds: BoundingBox) {
        super.addToBoundingBox(bounds)
        instanceOf?.let { bounds.add(it.mesh.geometry.bounds) }
    }

    override fun forEachMeshPrimitive(action: (MeshPrimitive) -> Unit) {
        instanceOf?.markInstanceVisible(this)
        instanceOf?.let(action)
        children.fastForEach { it.forEachMeshPrimitive(action) }
    }

    override fun forEachMeshPrimitive(camera: Camera, action: (MeshPrimitive) -> Unit) {
        if (frustumCulled) {
            val instanceOf = instanceOf
            if (instanceOf != null) {
                val inFrustum =
                    camera.sphereInFrustum(globalBoundsSphere.center, globalBoundsSphere.radius)
                if (inFrustum) {
                    instanceOf.markInstanceVisible(this)
                    action(instanceOf)
                }
            }
        } else {
            instanceOf?.markInstanceVisible(this)
            instanceOf?.let(action)
        }
        children.fastForEach { it.forEachMeshPrimitive(camera, action) }
    }

    /**
     * Copying a [MeshPrimitiveInstance] will reuse the underlying primitive, if applicable. In
     * other words, this will create an instance of the underlying geometry. Just the fact of
     * copying this node will update the [MeshPrimitive] instance data. This node will still need to
     * have [update] called and passed into the renderer to handle any frustum related updates.
     */
    override fun copy(): Node3D {
        val copy =
            MeshPrimitiveInstance().also {
                it.name = name
                it.frustumCulled = frustumCulled
                it.globalTransform = globalTransform
                instanceOf?.let { instance -> it.addTo(instance) }
            }
        children.fastForEach { child -> copy.addChild(child.copy()) }
        return copy
    }

    override fun onDestroy() {
        super.onDestroy()
        remove()
    }
}
