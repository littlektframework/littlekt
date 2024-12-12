package com.littlekt.graphics.g3d

import com.littlekt.graphics.IndexedMesh
import com.littlekt.graphics.g3d.util.MaterialPipeline
import com.littlekt.graphics.g3d.util.MaterialPipelineProvider
import com.littlekt.graphics.g3d.util.MaterialPipelineSorter
import com.littlekt.graphics.util.IndexedMeshGeometry
import com.littlekt.graphics.webgpu.Device
import com.littlekt.graphics.webgpu.IndexFormat
import com.littlekt.graphics.webgpu.RenderPassEncoder
import com.littlekt.math.Mat4

/**
 * @author Colton Daily
 * @date 12/7/2024
 */
class ModelBatch(val device: Device) {
    private val pipelineProviders = mutableSetOf<MaterialPipelineProvider>()
    private val pipelines = mutableListOf<MaterialPipeline>()
    private val meshesByPipeline = mutableMapOf<MaterialPipeline, MutableList<MeshNode>>()
    private val sorter =
        object : MaterialPipelineSorter {
            override fun sort(pipelines: MutableList<MaterialPipeline>) {}
        }
    private val dataMap = mutableMapOf<String, Any>()

    fun addPipelineProvider(provider: MaterialPipelineProvider) {
        pipelineProviders += provider
    }

    fun removePipelineProvider(provider: MaterialPipelineProvider) {
        pipelineProviders -= provider
    }

    fun render(meshNode: MeshNode) {
        run getPipeline@{
            var pipelineFound = false
            pipelineProviders.forEach { pipelineProvider ->
                val pipeline = pipelineProvider.getMaterialPipeline(device, meshNode)
                if (pipeline != null) {
                    pipelineFound = true
                    if (!pipelines.contains(pipeline)) {
                        pipelines += pipeline
                    }
                    meshesByPipeline.getOrPut(pipeline) { mutableListOf() }.apply { add(meshNode) }
                    return@getPipeline
                }
            }
            if (!pipelineFound) error("Unable to find pipeline for given meshNode!")
        }
    }

    fun flush(renderPassEncoder: RenderPassEncoder, viewProjection: Mat4?) {
        sorter.sort(pipelines)

        pipelines.forEach { pipeline ->
            val meshNodes = meshesByPipeline[pipeline]
            if (!meshNodes.isNullOrEmpty()) {
                renderPassEncoder.setPipeline(pipeline.renderPipeline)
                val shader = pipeline.shader
                dataMap.clear()
                shader.setBindGroups(renderPassEncoder, pipeline.bindGroups)
                meshNodes.forEach { meshNode ->
                    val mesh = meshNode.mesh
                    val indexedMesh = mesh as? IndexedMesh
                    indexedMesh?.let {
                        renderPassEncoder.setIndexBuffer(
                            mesh.ibo,
                            indexFormat = meshNode.stripIndexFormat ?: IndexFormat.UINT16,
                        )
                    }
                    renderPassEncoder.setVertexBuffer(0, mesh.vbo)

                    if (indexedMesh != null) {
                        val geometry = indexedMesh.geometry as IndexedMeshGeometry
                        renderPassEncoder.drawIndexed(geometry.numIndices, 1)
                    } else {
                        renderPassEncoder.draw(mesh.geometry.numVertices, 1)
                    }
                }
            }
        }

        pipelines.clear()
        meshesByPipeline.clear()
    }
}
