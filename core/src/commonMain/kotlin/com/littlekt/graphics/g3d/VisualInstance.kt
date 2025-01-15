package com.littlekt.graphics.g3d

/**
 * @author Colton Daily
 * @date 1/15/2025
 */
open class VisualInstance : Node3D() {

    var instanceOf: MeshNode? = null

    override fun dirty() {
        super.dirty()
        instanceOf?.instanceDirty(this)
    }

    fun addTo(meshNode: MeshNode) {
        meshNode.addInstance(this)
    }

    fun remove() {
        instanceOf?.removeInstance(this)
    }
}
