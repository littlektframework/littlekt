package com.littlekt.graphics.g3d

/**
 * @author Colton Daily
 * @date 11/24/2024
 */
open class Model : VisualInstance() {
    val nodes = mutableMapOf<String, Node3D>()
    val meshes = mutableMapOf<String, MeshNode>()
    val skins = mutableListOf<Skin>()
}
