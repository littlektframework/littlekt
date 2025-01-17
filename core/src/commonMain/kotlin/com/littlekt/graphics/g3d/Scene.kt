package com.littlekt.graphics.g3d

/**
 * @author Colton Daily
 * @date 1/17/2025
 */
open class Scene : Node3D() {
    var modelInstances = mutableListOf<ModelInstance>()
    var skins = mutableListOf<Skin>()

    fun createInstance(): Scene {
        val newInstance = Scene()
        val newModelInstances = modelInstances.map { it.createInstance() }
        newInstance.modelInstances += newModelInstances
        newModelInstances.forEach { newInstance += it }
        newInstance.skins = skins.toMutableList()
        return newInstance
    }
}
