package com.littlekt.graphics.g3d

import com.littlekt.graphics.Mesh

/**
 * @author Colton Daily
 * @date 11/24/2024
 */
open class Model : Node3D() {
    val nodes = mutableMapOf<String, Node3D>()
    val meshes = mutableMapOf<String, Mesh<*>>()
    val skins = mutableListOf<Skin>()
}
