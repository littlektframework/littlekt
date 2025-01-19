package com.littlekt.graphics.g3d

import com.littlekt.graphics.Color
import com.littlekt.util.datastructure.fastForEach

/**
 * @author Colton Daily
 * @date 1/17/2025
 */
open class Scene : Node3D() {
    var modelInstances = mutableListOf<ModelInstance>()
    var skins = mutableListOf<Skin>()

    fun setColor(color: Color) {
        modelInstances.forEach { it.setColor(color) }
    }

    /** Creates a new [Scene], along with any children [ModelInstance]. */
    override fun copy(): Scene {
        val copy =
            Scene().also {
                it.name = name
                it.globalTransform = globalTransform
            }
        children.fastForEach { child -> copy.addChild(child.copy()) }
        copy.modelInstances += copy.filterChildrenByType(ModelInstance::class)
        copy.skins = skins.toMutableList()
        return copy
    }
}
