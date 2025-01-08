package com.littlekt.graphics.g3d

import com.littlekt.graphics.IndexedMesh
import com.littlekt.graphics.Mesh
import com.littlekt.graphics.g3d.material.Material
import com.littlekt.graphics.webgpu.IndexFormat
import com.littlekt.graphics.webgpu.PrimitiveTopology

/**
 * @author Colton Daily
 * @date 11/25/2024
 */
open class VisualInstance(
    val mesh: Mesh<*>,
    val material: Material,
    val topology: PrimitiveTopology = PrimitiveTopology.TRIANGLE_LIST,
    val stripIndexFormat: IndexFormat? = null,
) : Node3D() {
    val indexedMesh = mesh as? IndexedMesh<*>

    init {
        if (
            topology == PrimitiveTopology.TRIANGLE_STRIP || topology == PrimitiveTopology.LINE_STRIP
        ) {
            check(stripIndexFormat != null) {
                error("VisualInstance.stripIndexFormat is required to be set for strip topologies!")
            }
        }
    }
}
