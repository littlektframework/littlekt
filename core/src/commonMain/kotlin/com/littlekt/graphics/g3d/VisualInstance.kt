package com.littlekt.graphics.g3d

import com.littlekt.util.datastructure.fastForEach

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

    override fun copy(): Node3D {
        val copy =
            VisualInstance().also {
                it.name = name
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
