package com.littlekt.graphics.g3d

/**
 * @author Colton Daily
 * @date 1/15/2025
 */
open class ModelInstance(val instanceOf: Model) : Node3D() {

    init {
        instanceOf.primitives.forEach { prim -> addChild(VisualInstance().apply { addTo(prim) }) }
    }

    fun createInstance() = ModelInstance(instanceOf)
}
