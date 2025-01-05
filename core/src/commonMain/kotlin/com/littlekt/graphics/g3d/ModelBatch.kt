package com.littlekt.graphics.g3d

import com.littlekt.graphics.Camera
import com.littlekt.graphics.g3d.shader.UnlitShader
import com.littlekt.graphics.g3d.util.CameraBuffers
import com.littlekt.graphics.g3d.util.MaterialPipeline
import com.littlekt.graphics.g3d.util.MaterialPipelineProvider
import com.littlekt.graphics.g3d.util.MaterialPipelineSorter
import com.littlekt.graphics.webgpu.Device
import com.littlekt.graphics.webgpu.IndexFormat
import com.littlekt.graphics.webgpu.RenderPassEncoder
import com.littlekt.graphics.webgpu.TextureFormat

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
            override fun sort(pipelines: MutableList<MaterialPipeline>) {
                pipelines.sort()
            }
        }
    private val dataMap = mutableMapOf<String, Any>()
    var colorFormat: TextureFormat = TextureFormat.RGBA8_UNORM
    var depthFormat: TextureFormat = TextureFormat.DEPTH24_PLUS_STENCIL8

    fun addPipelineProvider(provider: MaterialPipelineProvider) {
        pipelineProviders += provider
    }

    fun removePipelineProvider(provider: MaterialPipelineProvider) {
        pipelineProviders -= provider
    }

    fun render(model: Model, cameraBuffers: CameraBuffers) {
        model.meshes.values.forEach { render(it, cameraBuffers) }
    }

    fun render(meshNode: MeshNode, cameraBuffers: CameraBuffers) {
        run getPipeline@{
            var pipelineFound = false
            pipelineProviders.forEach { pipelineProvider ->
                val pipeline =
                    pipelineProvider.getMaterialPipeline(
                        device,
                        cameraBuffers,
                        meshNode,
                        colorFormat,
                        depthFormat,
                    )
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

    fun flush(renderPassEncoder: RenderPassEncoder, camera: Camera, cameraBuffers: CameraBuffers) {
        sorter.sort(pipelines)

        cameraBuffers.updateCameraUniform(camera)

        pipelines.forEach { pipeline ->
            val meshNodes = meshesByPipeline[pipeline]
            if (!meshNodes.isNullOrEmpty()) {
                renderPassEncoder.setPipeline(pipeline.renderPipeline)
                val shader = pipeline.shader
                dataMap.clear()
                meshNodes.forEach { meshNode ->
                    dataMap[UnlitShader.MODEL] = meshNode.globalTransform
                    shader.update(dataMap)
                    shader.setBindGroups(renderPassEncoder, pipeline.bindGroups)
                    val mesh = meshNode.mesh
                    val indexedMesh = meshNode.indexedMesh
                    indexedMesh?.let {
                        renderPassEncoder.setIndexBuffer(
                            indexedMesh.ibo,
                            indexFormat = meshNode.stripIndexFormat ?: IndexFormat.UINT16,
                        )
                    }
                    renderPassEncoder.setVertexBuffer(0, mesh.vbo)

                    if (indexedMesh != null) {
                        renderPassEncoder.drawIndexed(indexedMesh.geometry.numIndices, 1)
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
