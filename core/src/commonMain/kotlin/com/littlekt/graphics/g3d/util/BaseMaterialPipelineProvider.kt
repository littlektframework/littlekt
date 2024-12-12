package com.littlekt.graphics.g3d.util

import com.littlekt.graphics.VertexBufferLayout
import com.littlekt.graphics.g3d.MeshNode
import com.littlekt.graphics.g3d.material.Material
import com.littlekt.graphics.webgpu.Device
import com.littlekt.graphics.webgpu.IndexFormat
import com.littlekt.graphics.webgpu.PrimitiveTopology
import com.littlekt.graphics.webgpu.VertexStepMode

/**
 * @author Colton Daily
 * @date 12/8/2024
 */
abstract class BaseMaterialPipelineProvider<T : Material> : MaterialPipelineProvider {
    private val pipelines = mutableMapOf<RenderInfo, MaterialPipeline>()
    private var renderInfo = RenderInfo()

    override fun getMaterialPipeline(device: Device, meshNode: MeshNode): MaterialPipeline? {
        @Suppress("UNCHECKED_CAST") val material = meshNode.material as? T
        if (material != null) {
            renderInfo.apply {
                reset()
                this.material = material
                layout = meshNode.mesh.geometry.layout
                topology = meshNode.topology
                indexFormat = meshNode.stripIndexFormat
            }
            if (pipelines.contains(renderInfo)) {
                return pipelines[renderInfo]
            } else {
                val pipeline =
                    createMaterialPipeline(
                        device,
                        meshNode.mesh.geometry.layout,
                        meshNode.topology,
                        material,
                    )

                pipelines[renderInfo.copy()] = pipeline
                return pipeline
            }
        }
        return null
    }

    abstract fun createMaterialPipeline(
        device: Device,
        layout: VertexBufferLayout,
        topology: PrimitiveTopology,
        material: T,
    ): MaterialPipeline

    private data class RenderInfo(
        var material: Material = object : Material() {},
        var layout: VertexBufferLayout = VertexBufferLayout(0, VertexStepMode.VERTEX, emptyList()),
        var topology: PrimitiveTopology = PrimitiveTopology.TRIANGLE_LIST,
        var indexFormat: IndexFormat? = null,
    ) {
        fun reset() {
            material = object : Material() {}
            layout = VertexBufferLayout(0, VertexStepMode.VERTEX, emptyList())
            topology = PrimitiveTopology.TRIANGLE_LIST
            indexFormat = null
        }
    }
}
