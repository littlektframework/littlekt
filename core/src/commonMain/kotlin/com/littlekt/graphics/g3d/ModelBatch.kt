package com.littlekt.graphics.g3d

import com.littlekt.Releasable
import com.littlekt.graphics.Camera
import com.littlekt.graphics.g3d.util.MaterialPipeline
import com.littlekt.graphics.g3d.util.MaterialPipelineProvider
import com.littlekt.graphics.g3d.util.MaterialPipelineSorter
import com.littlekt.graphics.webgpu.*
import com.littlekt.log.Logger

/**
 * @author Colton Daily
 * @date 12/7/2024
 */
class ModelBatch(val device: Device, size: Int = 128) : Releasable {
    private val pipelineProviders = mutableSetOf<MaterialPipelineProvider>()
    private val pipelines = mutableListOf<MaterialPipeline>()
    private val instancesByPipeline = mutableMapOf<MaterialPipeline, MutableList<VisualInstance>>()

    /** By material id */
    private val bindGroupsByMaterial = mutableMapOf<Int, List<BindGroup>>()
    private val updatedEnvironments = mutableSetOf<Int>()
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

    fun render(model: Model, environment: Environment) {
        model.meshes.values.forEach { render(it, environment) }
    }

    fun render(instance: VisualInstance, environment: Environment) {
        run getPipeline@{
            var pipelineFound = false
            pipelineProviders.forEach { pipelineProvider ->
                val pipeline =
                    pipelineProvider.getMaterialPipeline(
                        device,
                        instance.material,
                        environment,
                        instance.mesh.geometry.layout,
                        instance.topology,
                        instance.stripIndexFormat,
                        colorFormat,
                        depthFormat,
                    )
                if (pipeline != null) {
                    pipelineFound = true
                    if (!pipelines.contains(pipeline)) {
                        pipelines += pipeline
                    }
                    // todo - pool lists?
                    instancesByPipeline
                        .getOrPut(pipeline) { mutableListOf() }
                        .apply { add(instance) }

                    bindGroupsByMaterial.getOrPut(instance.material.id) {
                        listOf(environment.buffers.bindGroup) +
                            instance.material.createBindGroups(pipeline.shader.layouts)
                    }
                    return@getPipeline
                }
            }
            if (!pipelineFound) error("Unable to find pipeline for given meshNode!")
        }
    }

    fun flush(renderPassEncoder: RenderPassEncoder, camera: Camera) {
        sorter.sort(pipelines)

        pipelines.forEach { pipeline ->
            // we only need to update the camera buffers in each environment once. So if we are
            // sharing environment, just update the first instance of it.
            if (updatedEnvironments.add(pipeline.environment.id)) {
                pipeline.environment.updateCameraBuffers(camera)
            }
            val visualInstances = instancesByPipeline[pipeline]
            if (!visualInstances.isNullOrEmpty()) {
                val shader = pipeline.shader
                renderPassEncoder.setPipeline(pipeline.renderPipeline)
                dataMap.clear()
                // TODO need a way to cache bind groups so we aren't setting the same ones over and
                // over
                visualInstances.forEach { visualInstance ->
                    val bindGroups =
                        bindGroupsByMaterial[visualInstance.material.id]
                            ?: error(
                                "Material (${visualInstance.material.id}) bind groups could not be found!"
                            )
                    shader.setBindGroups(renderPassEncoder, bindGroups)
                    visualInstance.material.update(visualInstance.globalTransform)
                    val mesh = visualInstance.mesh
                    val indexedMesh = visualInstance.indexedMesh

                    indexedMesh?.let {
                        renderPassEncoder.setIndexBuffer(
                            indexedMesh.ibo,
                            indexFormat = visualInstance.stripIndexFormat ?: IndexFormat.UINT16,
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
        instancesByPipeline.clear()
        updatedEnvironments.clear()
    }

    override fun release() {
        pipelineProviders.forEach { it.release() }
        bindGroupsByMaterial.values.forEach { bindGroups -> bindGroups.forEach { it.release() } }
    }

    companion object {
        private val logger = Logger<ModelBatch>()
    }
}
