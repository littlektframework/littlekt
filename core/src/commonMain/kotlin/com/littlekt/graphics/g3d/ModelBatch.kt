package com.littlekt.graphics.g3d

import com.littlekt.Releasable
import com.littlekt.graphics.Camera
import com.littlekt.graphics.g3d.material.Material
import com.littlekt.graphics.g3d.util.BaseMaterialPipelineProvider
import com.littlekt.graphics.g3d.util.MaterialPipeline
import com.littlekt.graphics.g3d.util.MaterialPipelineProvider
import com.littlekt.graphics.g3d.util.MaterialPipelineSorter
import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.webgpu.*
import com.littlekt.log.Logger
import kotlin.reflect.KClass
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 12/7/2024
 */
class ModelBatch(val device: Device, size: Int = 128) : Releasable {
    private val pipelineProviders: MutableMap<KClass<out Material>, MaterialPipelineProvider> =
        mutableMapOf()
    private val pipelines = mutableListOf<MaterialPipeline>()
    private val meshNodesByPipeline = mutableMapOf<MaterialPipeline, MutableList<MeshNode>>()

    /** By material id */
    private val bindGroupByMaterialId = mutableMapOf<Int, BindGroup>()
    private val updatedEnvironments = mutableSetOf<Int>()
    private val sorter =
        object : MaterialPipelineSorter {
            override fun sort(pipelines: MutableList<MaterialPipeline>) {
                pipelines.sort()
            }
        }
    var colorFormat: TextureFormat = TextureFormat.RGBA8_UNORM
    var depthFormat: TextureFormat = TextureFormat.DEPTH24_PLUS_STENCIL8

    inline fun <reified T : Material> addPipelineProvider(provider: MaterialPipelineProvider) =
        addPipelineProvider(T::class, provider)

    fun <T : Material> addPipelineProvider(type: KClass<T>, provider: MaterialPipelineProvider) {
        !pipelineProviders.containsKey(type) ||
            error("MaterialProvider already registered to type: $type")
        pipelineProviders[type] = provider
    }

    inline fun <reified T : Material> removePipelineProvider(): T? =
        removePipelineProvider(T::class)

    @Suppress("UNCHECKED_CAST")
    fun <T : Material> removePipelineProvider(type: KClass<T>): T? =
        pipelineProviders.remove(type) as T?

    fun addPipelineProvider(provider: BaseMaterialPipelineProvider<*>) =
        addPipelineProvider(provider.type, provider)

    fun removePipelineProvider(provider: BaseMaterialPipelineProvider<*>) =
        removePipelineProvider(provider.type)

    fun render(model: Model, environment: Environment) {
        model.meshes.values.forEach { render(it, environment) }
    }

    fun render(instance: MeshNode, environment: Environment) {
        val pipeline =
            pipelineProviders[instance.material::class]?.getMaterialPipeline(
                device,
                instance.material,
                environment,
                instance.mesh.geometry.layout,
                instance.topology,
                instance.stripIndexFormat,
                colorFormat,
                depthFormat,
            ) ?: error("Unable to find pipeline for given instance!")
        if (!pipelines.contains(pipeline)) {
            pipelines += pipeline
        }
        // todo - pool lists?
        meshNodesByPipeline.getOrPut(pipeline) { mutableListOf() }.apply { add(instance) }

        bindGroupByMaterialId.getOrPut(instance.material.id) {
            instance.material.createBindGroup(pipeline.shader)
        }
    }

    fun flush(renderPassEncoder: RenderPassEncoder, camera: Camera, dt: Duration) {
        sorter.sort(pipelines)
        var lastEnvironmentSet: Environment? = null
        var lastMaterialSet: Material? = null
        pipelines.forEach { pipeline ->
            // we only need to update the camera buffers in each environment once. So if we are
            // sharing environment, just update the first instance of it.
            if (updatedEnvironments.add(pipeline.environment.id)) {
                pipeline.environment.update(camera, dt)
            }
            if (lastEnvironmentSet != pipeline.environment) {
                lastEnvironmentSet = pipeline.environment
                pipeline.shader.setBindGroup(
                    renderPassEncoder,
                    pipeline.environment.buffers.bindGroup,
                    BindingUsage.CAMERA,
                )
            }
            val meshNodes = meshNodesByPipeline[pipeline]
            if (!meshNodes.isNullOrEmpty()) {
                renderPassEncoder.setPipeline(pipeline.renderPipeline)
                meshNodes.forEach { meshNode ->
                    val materialBindGroup =
                        bindGroupByMaterialId[meshNode.material.id]
                            ?: error(
                                "Material (${meshNode.material.id}) bind groups could not be found!"
                            )
                    if (lastMaterialSet != meshNode.material) {
                        lastMaterialSet = meshNode.material
                        pipeline.shader.setBindGroup(
                            renderPassEncoder,
                            materialBindGroup,
                            BindingUsage.MATERIAL,
                        )
                    }
                    meshNode.material.update()
                    meshNode.writeInstanceDataToBuffer()
                    pipeline.shader.setBindGroup(
                        renderPassEncoder,
                        meshNode.instanceBuffers.getOrCreateBindGroup(
                            pipeline.shader.getBindGroupLayoutByUsage(BindingUsage.MODEL)
                        ),
                        BindingUsage.MODEL,
                    )
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
                        renderPassEncoder.drawIndexed(
                            indexedMesh.geometry.numIndices,
                            meshNode.instanceCount,
                        )
                    } else {
                        renderPassEncoder.draw(mesh.geometry.numVertices, meshNode.instanceCount)
                    }
                }
            }
        }

        pipelines.clear()
        meshNodesByPipeline.clear()
        updatedEnvironments.clear()
    }

    override fun release() {
        pipelineProviders.values.forEach { it.release() }
        bindGroupByMaterialId.values.forEach { it.release() }
    }

    companion object {
        private val logger = Logger<ModelBatch>()
    }
}
