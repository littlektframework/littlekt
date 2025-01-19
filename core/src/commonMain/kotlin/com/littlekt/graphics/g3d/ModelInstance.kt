package com.littlekt.graphics.g3d

import com.littlekt.util.datastructure.fastForEach

/**
 * @author Colton Daily
 * @date 1/15/2025
 */
open class ModelInstance(val instanceOf: Model) : Node3D() {

    fun createVisualInstances() {
        instanceOf.primitives.forEach { prim -> addChild(VisualInstance().apply { addTo(prim) }) }
    }

    /** Create a new [ModelInstance] and any children [VisualInstance]. */
    override fun copy(): Node3D {
        val copy =
            ModelInstance(instanceOf).also {
                it.name = name
                it.globalTransform = globalTransform
            }
        children.fastForEach { child -> copy.addChild(child.copy()) }
        return copy
    }
}
