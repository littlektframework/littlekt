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
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.datastructure.pool
import kotlin.math.max
import kotlin.reflect.KClass
import kotlin.time.Duration

/**
 * A rendering helper class to handle pipeline caching and bind group caching for drawing 3D meshes.
 *
 * @author Colton Daily
 * @date 12/7/2024
 */
class ShadowModelBatch(val device: Device) : Releasable {
    private val pipelineProviders = mutableMapOf<KClass<out Material>, MaterialPipelineProvider>()
    private val pipelines = mutableListOf<MaterialPipeline>()
    private val primitivesByPipeline = mutableMapOf<MaterialPipeline, MutableList<MeshPrimitive>>()
    private val listPool = pool(reset = { it.clear() }) { mutableListOf<MeshPrimitive>() }

    private val bindGroupByMaterialId = mutableMapOf<Int, BindGroup>()
    private val bindGroupBySkinId = mutableMapOf<Int, BindGroup>()
    private val updatedEnvironments = mutableSetOf<Int>()
    private val sorter =
        object : MaterialPipelineSorter {
            override fun sort(pipelines: MutableList<MaterialPipeline>) {
                pipelines.sort()
            }
        }

    /** The [TextureFormat] of the color attachment. This is used in pipeline caching. */
    var colorFormat: TextureFormat = TextureFormat.RGBA8_UNORM

    /** The [TextureFormat] of the depth attachment. This is used in pipeline caching. */
    var depthFormat: TextureFormat = TextureFormat.DEPTH24_PLUS_STENCIL8

    /** Add a new [MaterialPipelineProvider]. */
    inline fun <reified T : Material> addPipelineProvider(provider: MaterialPipelineProvider) =
        addPipelineProvider(T::class, provider)

    /** Add a new [MaterialPipelineProvider]. */
    fun <T : Material> addPipelineProvider(type: KClass<T>, provider: MaterialPipelineProvider) {
        !pipelineProviders.containsKey(type) ||
            error("MaterialProvider already registered to type: $type")
        pipelineProviders[type] = provider
    }

    /** Removes a [MaterialPipelineProvider] of the given type. */
    inline fun <reified T : Material> removePipelineProvider(): T? =
        removePipelineProvider(T::class)

    /** Removes a [MaterialPipelineProvider] of the given type. */
    @Suppress("UNCHECKED_CAST")
    fun <T : Material> removePipelineProvider(type: KClass<T>): T? =
        pipelineProviders.remove(type) as T?

    /** Add a new [BaseMaterialPipelineProvider]. */
    fun addPipelineProvider(provider: BaseMaterialPipelineProvider<*>) =
        addPipelineProvider(provider.type, provider)

    /** Remove a [BaseMaterialPipelineProvider]. */
    fun removePipelineProvider(provider: BaseMaterialPipelineProvider<*>) =
        removePipelineProvider(provider.type)

    /**
     * Prepare a [Node3D] for rendering by creating the pipeline and material bind groups for the
     * specified [Environment]. This is useful if a large scene needs loading and generated on a
     * separate thread to prevent frames from dropping.
     *
     * **Note:** This does nothing if the given scene has the pipeline prepared either via
     * [preparePipeline] or [render].
     */
    fun preparePipeline(node: Node3D, environment: Environment) {
        node.forEachMeshPrimitive { preparePipeline(it, environment) }
    }

    /**
     * Prepare a [MeshPrimitive] for rendering by creating the pipeline and material bind groups for
     * the specified [Environment].This is useful if a large mesh primitive needs loading and
     * generated on a separate thread to prevent frames from dropping.
     *
     * **Note:** This does nothing if the given mesh primitive has the pipeline prepared either via
     * [preparePipeline] or [render].
     */
    fun preparePipeline(meshPrimitive: MeshPrimitive, environment: Environment) {
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
        if (meshPrimitive.material.ready) {
            bindGroupByMaterialId.getOrPutNotNull(meshPrimitive.material.id) {
                meshPrimitive.material.createBindGroup(pipeline.shader)
            }
        }
    }

    /** Adds a [Node3D] to be drawn on the next [flush] using the specified [Environment].. */
    fun render(node: Node3D, environment: Environment) {
        node.forEachMeshPrimitive { render(it, environment) }
    }

    /**
     * Adds a [Node3D] to be drawn on the next [flush] using the specified [Environment] with the
     * given material override.
     */
    fun render(node: Node3D, material: Material, environment: Environment) {
        node.forEachMeshPrimitive { render(it, material, environment) }
    }

    /**
     * Render any mesh primitives with the given node using the given [Camera] to attempt any
     * frustum culling.
     */
    fun render(node: Node3D, camera: Camera, environment: Environment) {
        node.forEachMeshPrimitive(camera) { render(it, environment) }
    }

    /**
     * Render any mesh primitives with the given node using the given [Camera] to attempt any
     * frustum culling with an material override.
     */
    fun render(node: Node3D, camera: Camera, material: Material, environment: Environment) {
        node.forEachMeshPrimitive(camera) { render(it, material, environment) }
    }

    /** Adds a [MeshPrimitive] to be drawn on the next [flush] using the specified [Environment]. */
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
        if (meshPrimitive.material.ready) {
            if (!pipelines.contains(pipeline)) {
                pipelines += pipeline
            }
            primitivesByPipeline
                .getOrPut(pipeline) { listPool.alloc() }
                .apply { add(meshPrimitive) }

            bindGroupByMaterialId.getOrPutNotNull(meshPrimitive.material.id) {
                meshPrimitive.material.createBindGroup(pipeline.shader)
            }
        }
        val skin = meshPrimitive.skin
        if (meshPrimitive.material.skinned && skin != null) {
            bindGroupBySkinId.getOrPut(skin.id) { skin.createBindGroup(pipeline.shader) }
        }
    }

    /** Render a [MeshPrimitive] by overriding the attached material with the given material. */
    fun render(meshPrimitive: MeshPrimitive, material: Material, environment: Environment) {
        val pipeline =
            pipelineProviders[material::class]?.getMaterialPipeline(
                device,
                material,
                environment,
                meshPrimitive.mesh.geometry.layout,
                meshPrimitive.topology,
                meshPrimitive.stripIndexFormat,
                colorFormat,
                depthFormat,
            ) ?: error("Unable to find pipeline for given instance!")
        if (material.ready) {
            if (!pipelines.contains(pipeline)) {
                pipelines += pipeline
            }
            primitivesByPipeline
                .getOrPut(pipeline) { listPool.alloc() }
                .apply { add(meshPrimitive) }

            bindGroupByMaterialId.getOrPutNotNull(material.id) {
                material.createBindGroup(pipeline.shader)
            }
        }
        val skin = meshPrimitive.skin
        if (material.skinned && skin != null) {
            bindGroupBySkinId.getOrPut(skin.id) { skin.createBindGroup(pipeline.shader) }
        }
    }

    fun flush(renderPassEncoder: RenderPassEncoder, camera: Camera, dt: Duration) {
        sorter.sort(pipelines)
        var lastEnvironmentSet: Environment? = null
        var lastMaterialSet: Int? = null
        pipelines.fastForEach { pipeline ->
            // we only need to update the camera buffers in each environment once. So if we are
            // sharing environment, just update the first instance of it.
            if (updatedEnvironments.add(pipeline.environment.id)) {
                pipeline.environment.update(camera, dt)
            }
            if (lastEnvironmentSet != pipeline.environment) {
                lastEnvironmentSet = pipeline.environment
                pipeline.shader.setBindGroup(
                    renderPassEncoder,
                    pipeline.environment.buffers.getOrCreateBindGroup(pipeline.shader),
                    pipeline.environment.buffers.bindingUsage,
                )
            }
            val primitives = primitivesByPipeline[pipeline]
            if (!primitives.isNullOrEmpty()) {
                renderPassEncoder.setPipeline(pipeline.renderPipeline)
                primitives.fastForEach primitives@{ primitive ->
                    if (primitive.visibleInstanceCount <= 0) return@primitives
                    val materialBindGroup = bindGroupByMaterialId[primitive.material.id]

                    if (primitive.material.skinned) {
                        val skinBindGroup =
                            primitive.skin?.let { bindGroupBySkinId[it.id] }
                                ?: error("Skin bind groups could not be found!")
                        pipeline.shader.setBindGroup(
                            renderPassEncoder,
                            skinBindGroup,
                            BindingUsage.SKIN,
                        )
                        primitive.skin?.writeToBuffer()
                    }
                    if (lastMaterialSet != primitive.material.id) {
                        lastMaterialSet = primitive.material.id
                        materialBindGroup?.let {
                            pipeline.shader.setBindGroup(
                                renderPassEncoder,
                                it,
                                BindingUsage.MATERIAL,
                            )
                        }
                        primitive.material.update()
                    }
                    primitive.writeInstanceDataToBuffer()
                    pipeline.shader.setBindGroup(
                        renderPassEncoder,
                        primitive.instanceBuffers.getOrCreateBindGroup(
                            pipeline.shader.getBindGroupLayoutByUsage(BindingUsage.MODEL)
                        ),
                        BindingUsage.MODEL,
                    )
                    val mesh = primitive.mesh
                    val indexedMesh = primitive.indexedMesh

                    indexedMesh?.let {
                        renderPassEncoder.setIndexBuffer(
                            indexedMesh.ibo,
                            indexFormat = primitive.stripIndexFormat ?: IndexFormat.UINT16,
                        )
                    }
                    renderPassEncoder.setVertexBuffer(0, mesh.vbo)

                    if (indexedMesh != null) {
                        EngineStats.extra(
                            INSTANCED_STAT_NAME,
                            max(0, primitive.visibleInstanceCount - 1),
                        )
                        EngineStats.extra(DRAW_CALLS_STAT_NAME, 1)
                        renderPassEncoder.drawIndexed(
                            indexedMesh.geometry.numIndices,
                            primitive.visibleInstanceCount,
                        )
                    } else {
                        EngineStats.extra(
                            INSTANCED_STAT_NAME,
                            max(0, primitive.visibleInstanceCount - 1),
                        )
                        EngineStats.extra(DRAW_CALLS_STAT_NAME, 1)
                        renderPassEncoder.draw(
                            mesh.geometry.numVertices,
                            primitive.visibleInstanceCount,
                        )
                    }
                    primitive.resetVisibility()
                }
            }
        }

        pipelines.clear()
        primitivesByPipeline.values.forEach {
            it.clear()
            listPool.free(it)
        }
        primitivesByPipeline.clear()
        updatedEnvironments.clear()
    }

    override fun release() {
        pipelineProviders.values.forEach { it.release() }
        bindGroupByMaterialId.values.forEach { it.release() }
    }

    private inline fun <K, V> MutableMap<K, V>.getOrPutNotNull(key: K, defaultValue: () -> V?): V? {
        val value = get(key)
        return if (value == null) {
            val answer = defaultValue()
            answer?.let { put(key, answer) }
        } else {
            value
        }
    }

    companion object {
        private val logger = Logger<ShadowModelBatch>()
        private const val INSTANCED_STAT_NAME = "ModelBatch instanced count"
        private const val DRAW_CALLS_STAT_NAME = "ModelBatch Draw calls"
    }
}
