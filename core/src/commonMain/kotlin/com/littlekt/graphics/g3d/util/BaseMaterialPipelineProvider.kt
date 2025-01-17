package com.littlekt.graphics.g3d.util

import com.littlekt.graphics.VertexBufferLayout
import com.littlekt.graphics.g3d.Environment
import com.littlekt.graphics.g3d.material.Material
import com.littlekt.graphics.webgpu.*
import kotlin.reflect.KClass

/**
 * @author Colton Daily
 * @date 12/8/2024
 */
abstract class BaseMaterialPipelineProvider<T : Material> : MaterialPipelineProvider {
    abstract val type: KClass<T>
    private val pipelines = mutableMapOf<RenderInfo, MaterialPipeline>()
    private var renderInfo = RenderInfo()

    override fun getMaterialPipeline(
        device: Device,
        material: Material,
        environment: Environment,
        layout: VertexBufferLayout,
        topology: PrimitiveTopology,
        stripIndexFormat: IndexFormat?,
        colorFormat: TextureFormat,
        depthFormat: TextureFormat,
    ): MaterialPipeline? {
        renderInfo.apply {
            reset()
            this.environment = environment
            this.material = material
            this.layout = layout
            this.topology = topology
            this.indexFormat = stripIndexFormat
        }
        if (pipelines.contains(renderInfo)) {
            return pipelines[renderInfo]
        } else {
            @Suppress("UNCHECKED_CAST")
            val pipeline =
                createMaterialPipeline(
                    device,
                    environment,
                    layout,
                    topology,
                    material as T,
                    colorFormat,
                    depthFormat,
                )

            pipelines[renderInfo.copy()] = pipeline
            return pipeline
        }
    }

    abstract fun createMaterialPipeline(
        device: Device,
        environment: Environment,
        layout: VertexBufferLayout,
        topology: PrimitiveTopology,
        material: T,
        colorFormat: TextureFormat,
        depthFormat: TextureFormat,
    ): MaterialPipeline

    override fun release() {
        pipelines.values.forEach { pipeline ->
            pipeline.renderPipeline.release()
            pipeline.shader.release()
        }
    }

    private data class RenderInfo(
        var material: Material? = null,
        var layout: VertexBufferLayout = VertexBufferLayout(0, VertexStepMode.VERTEX, emptyList()),
        var topology: PrimitiveTopology = PrimitiveTopology.TRIANGLE_LIST,
        var indexFormat: IndexFormat? = null,
        var colorFormat: TextureFormat = TextureFormat.RGBA8_UNORM,
        var depthFormat: TextureFormat = TextureFormat.DEPTH24_PLUS_STENCIL8,
        var environment: Environment? = null,
    ) {
        fun reset() {
            material = null
            layout = VertexBufferLayout(0, VertexStepMode.VERTEX, emptyList())
            topology = PrimitiveTopology.TRIANGLE_LIST
            indexFormat = null
            colorFormat = TextureFormat.RGBA8_UNORM
            depthFormat = TextureFormat.DEPTH24_PLUS_STENCIL8
            environment = null
        }
    }
}