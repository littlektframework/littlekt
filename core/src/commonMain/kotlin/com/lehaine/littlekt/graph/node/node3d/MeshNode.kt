package com.lehaine.littlekt.graph.node.node3d

import com.lehaine.littlekt.graph.node.render.ModelMaterial
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.Mesh
import com.lehaine.littlekt.graphics.g3d.model.Skin

/**
 * @author Colton Daily
 * @date 12/17/2022
 */
class MeshNode(val mesh: Mesh) : VisualInstance() {
    var morphWeights: FloatArray? = null
    var skin: Skin? = null
    var isOpaque = true

    override fun render(camera: Camera) {
        super.render(camera)
        val material = material ?: scene?.currentMaterial as? ModelMaterial ?: return
        material.shader?.let {
            mesh.render(it)
        }
    }
}