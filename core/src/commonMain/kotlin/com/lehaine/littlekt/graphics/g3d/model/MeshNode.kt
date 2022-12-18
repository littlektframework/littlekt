package com.lehaine.littlekt.graphics.g3d.model

import com.lehaine.littlekt.graphics.Mesh

/**
 * @author Colton Daily
 * @date 12/17/2022
 */
class MeshNode(val mesh: Mesh, name: String? = null) : Node(name) {
    var morphWeights: FloatArray? = null
    var skin: Skin? = null
    var isOpaque = true
}