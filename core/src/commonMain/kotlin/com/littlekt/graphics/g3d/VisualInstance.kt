package com.littlekt.graphics.g3d

/**
 * @author Colton Daily
 * @date 1/15/2025
 */
open class VisualInstance : Node3D() {

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

    override fun onDestroy() {
        super.onDestroy()
        remove()
    }
}
