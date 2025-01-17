package com.littlekt.graphics.g3d

import com.littlekt.EngineStats
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
import kotlin.math.max
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
    private val primitivesByPipeline = mutableMapOf<MaterialPipeline, MutableList<MeshPrimitive>>()

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

    fun render(scene: Scene, environment: Environment) {
        scene.modelInstances.forEach { render(it, environment) }
    }

    fun render(model: ModelInstance, environment: Environment) {
        model.instanceOf.primitives.forEach { render(it, environment) }
    }

    fun render(meshPrimitive: MeshPrimitive, environment: Environment) {
        val pipeline =
            pipelineProviders[meshPrimitive.material::class]?.getMaterialPipeline(
                device,
                meshPrimitive.material,
                environment,
                meshPrimitive.mesh.geometry.layout,
                meshPrimitive.topology,
                meshPrimitive.stripIndexFormat,
                colorFormat,
                depthFormat,
            ) ?: error("Unable to find pipeline for given instance!")
        if (!pipelines.contains(pipeline)) {
            pipelines += pipeline
        }
        // todo - pool lists?
        primitivesByPipeline.getOrPut(pipeline) { mutableListOf() }.apply { add(meshPrimitive) }

        bindGroupByMaterialId.getOrPut(meshPrimitive.material.id) {
            meshPrimitive.material.createBindGroup(pipeline.shader)
        }
    }

    fun flush(renderPassEncoder: RenderPassEncoder, camera: Camera, dt: Duration) {
        sorter.sort(pipelines)
        var lastEnvironmentSet: Environment? = null
        var lastMaterialSet: Int? = null
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
            val primitive = primitivesByPipeline[pipeline]
            if (!primitive.isNullOrEmpty()) {
                renderPassEncoder.setPipeline(pipeline.renderPipeline)
                primitive.forEach { meshNode ->
                    val materialBindGroup =
                        bindGroupByMaterialId[meshNode.material.id]
                            ?: error(
                                "Material (${meshNode.material.id}) bind groups could not be found!"
                            )
                    if (lastMaterialSet != meshNode.material.id) {
                        lastMaterialSet = meshNode.material.id
                        pipeline.shader.setBindGroup(
                            renderPassEncoder,
                            materialBindGroup,
                            BindingUsage.MATERIAL,
                        )
                        meshNode.material.update()
                    }
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
                        EngineStats.extra(INSTANCED_STAT_NAME, max(0, meshNode.instanceCount - 1))
                        EngineStats.extra(DRAW_CALLS_STAT_NAME, 1)
                        renderPassEncoder.drawIndexed(
                            indexedMesh.geometry.numIndices,
                            meshNode.instanceCount,
                        )
                    } else {
                        EngineStats.extra(INSTANCED_STAT_NAME, max(0, meshNode.instanceCount - 1))
                        EngineStats.extra(DRAW_CALLS_STAT_NAME, 1)
                        renderPassEncoder.draw(mesh.geometry.numVertices, meshNode.instanceCount)
                    }
                }
            }
        }

        pipelines.clear()
        primitivesByPipeline.clear()
        updatedEnvironments.clear()
    }

    override fun release() {
        pipelineProviders.values.forEach { it.release() }
        bindGroupByMaterialId.values.forEach { it.release() }
    }

    companion object {
        private val logger = Logger<ModelBatch>()
        private const val INSTANCED_STAT_NAME = "ModelBatch instanced count"
        private const val DRAW_CALLS_STAT_NAME = "ModelBatch Draw calls"
    }
}
