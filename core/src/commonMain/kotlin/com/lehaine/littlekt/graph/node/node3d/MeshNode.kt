package com.lehaine.littlekt.graph.node.node3d

import com.lehaine.littlekt.graphics.Mesh
import com.lehaine.littlekt.graphics.g3d.model.Skin
import com.lehaine.littlekt.graphics.shader.ShaderProgram

/**
 * @author Colton Daily
 * @date 12/17/2022
 */
class MeshNode(val mesh: Mesh) : Node3D() {
    var morphWeights: FloatArray? = null
    var skin: Skin? = null
    var isOpaque = true

    override fun render(shader: ShaderProgram<*, *>) {
        super.render(shader)
        mesh.render(shader)
    }
}