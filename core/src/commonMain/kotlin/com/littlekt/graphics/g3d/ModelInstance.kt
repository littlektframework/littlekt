package com.littlekt.graphics.g3d

/**
 * @author Colton Daily
 * @date 1/15/2025
 */
class ModelInstance(val instanceOf: Model) : Node3D() {
    val meshInstances = mutableListOf<VisualInstance>()
}
