package com.littlekt.graphics.g3d

import com.littlekt.graphics.IndexedMesh
import com.littlekt.graphics.Mesh
import com.littlekt.graphics.g3d.material.Material
import com.littlekt.graphics.webgpu.*

/**
 * @author Colton Daily
 * @date 11/25/2024
 */
open class MeshNode(
    val mesh: Mesh<*>,
    val material: Material,
    val topology: PrimitiveTopology = PrimitiveTopology.TRIANGLE_LIST,
    val stripIndexFormat: IndexFormat? = null,
) : VisualInstance() {
    val indexedMesh = mesh as? IndexedMesh<*>

    init {
        if (
            topology == PrimitiveTopology.TRIANGLE_STRIP || topology == PrimitiveTopology.LINE_STRIP
        ) {
            check(stripIndexFormat != null) {
                error("MeshNode.stripIndexFormat is required to be set for strip topologies!")
            }
        }
    }
}
