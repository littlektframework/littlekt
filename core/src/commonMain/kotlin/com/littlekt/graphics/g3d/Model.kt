package com.littlekt.graphics.g3d

import com.littlekt.math.Mat4

/**
 * @author Colton Daily
 * @date 11/24/2024
 */
open class Model : Node3D() {
    val nodes = mutableMapOf<String, Node3D>()
    val meshes = mutableMapOf<String, MeshNode>()
    val skins = mutableListOf<Skin>()

    val instances = mutableListOf<ModelInstance>()

    fun createModelInstance(): ModelInstance {
        val modelInstance = ModelInstance(this)
        meshes.values.forEach { mesh ->
            val meshInstance = VisualInstance()
            meshInstance.globalTransform = Mat4().set(mesh.globalTransform)
            mesh.addInstance(meshInstance)
            modelInstance += meshInstance
            modelInstance.meshInstances += meshInstance
        }
        instances += modelInstance
        return modelInstance
    }
}
