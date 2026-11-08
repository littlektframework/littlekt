package com.littlekt.graphics.g3d

import com.littlekt.EngineStats
import com.littlekt.Releasable
import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.Camera
import com.littlekt.graphics.g3d.material.Material
import com.littlekt.graphics.g3d.util.BaseMaterialPipelineProvider
import com.littlekt.graphics.g3d.util.InstanceBuffers
import com.littlekt.graphics.g3d.util.MaterialPipeline
import com.littlekt.graphics.g3d.util.MaterialPipelineProvider
import com.littlekt.graphics.g3d.util.MaterialPipelineSorter
import com.littlekt.graphics.util.CameraBuffersViaCamera
import com.littlekt.graphics.webgpu.*
import com.littlekt.util.datastructure.IntMap
import com.littlekt.util.datastructure.fastForEach
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.math.max
import kotlin.reflect.KClass
import kotlin.time.Duration

/**
 * A rendering helper class to handle pipeline caching and bind group caching for drawing 3D meshes.
 * A [ModelBatch] may be flushed multiple times within a single frame, such as for multiple passes
 * or cameras.
 *
 * @author Colton Daily
 * @date 12/7/2024
 */
class ModelBatch(val device: Device) : Releasable {
    private val pipelineProviders = mutableMapOf<KClass<out Material>, MaterialPipelineProvider>()
    private val pipelines = mutableListOf<MaterialPipeline>()

    private val bindGroupByMaterialId = IntMap<BindGroup>()
    private val bindGroupBySkinId = IntMap<BindGroup>()
    private val sorter =
        object : MaterialPipelineSorter {
            override fun sort(pipelines: MutableList<MaterialPipeline>) {
                pipelines.sort()
            }
        }

    private val instanceBuffers =
        InstanceBuffers(device, INITIAL_INSTANCES * MeshPrimitive.INSTANCE_DATA_SIZE)
    private var instanceStaging = FloatBuffer(INITIAL_INSTANCES * MeshPrimitive.INSTANCE_DATA_SIZE)
    private var instanceOffset = 0
    private var lastFrame = -1L
    private var flushId = nextFlushId()
    private var pipelineVersion = 0

    private var renderEnvironment: Environment? = null
    private var renderMaterial: Material? = null
    private val renderAction: (MeshPrimitive) -> Unit = {
        queue(it, renderMaterial ?: it.material, renderEnvironment ?: error("No environment set!"))
    }

    private val lock = SynchronizedObject()
    private val hasPendingBindGroups = atomic(false)
    private val preparedMaterialIds = mutableSetOf<Int>()
    private val pendingMaterialIds = mutableListOf<Int>()
    private val pendingMaterialBindGroups = mutableListOf<BindGroup>()

    /** The [TextureFormat] of the color attachment. This is used in pipeline caching. */
    var colorFormat: TextureFormat = TextureFormat.RGBA8_UNORM
        set(value) {
            if (field == value) return
            field = value
            pipelineVersion++
        }

    /** The [TextureFormat] of the depth attachment. This is used in pipeline caching. */
    var depthFormat: TextureFormat = TextureFormat.DEPTH24_PLUS_STENCIL8
        set(value) {
            if (field == value) return
            field = value
            pipelineVersion++
        }

    /**
     * The number of samples per pixel of the color & depth attachments for multisampling. This is
     * used in pipeline caching. Must be either `1` or `4`.
     */
    var sampleCount: Int = 1
        set(value) {
            require(value == 1 || value == 4) { "ModelBatch.sampleCount must be either 1 or 4!" }
            if (field == value) return
            field = value
            pipelineVersion++
        }

    /** Add a new [MaterialPipelineProvider]. */
    inline fun <reified T : Material> addPipelineProvider(provider: MaterialPipelineProvider) =
        addPipelineProvider(T::class, provider)

    /** Add a new [MaterialPipelineProvider]. */
    fun <T : Material> addPipelineProvider(type: KClass<T>, provider: MaterialPipelineProvider) {
        !pipelineProviders.containsKey(type) ||
            error("MaterialProvider already registered to type: $type")
        pipelineProviders[type] = provider
        pipelineVersion++
    }

    /** Removes a [MaterialPipelineProvider] of the given type. */
    inline fun <reified T : Material> removePipelineProvider(): T? =
        removePipelineProvider(T::class)

    /** Removes a [MaterialPipelineProvider] of the given type. */
    @Suppress("UNCHECKED_CAST")
    fun <T : Material> removePipelineProvider(type: KClass<T>): T? =
        (pipelineProviders.remove(type) as T?).also { pipelineVersion++ }

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
        val material = meshPrimitive.material
        val pipeline = lookupPipeline(meshPrimitive, material, environment)
        if (!material.ready) return
        val shouldCreate = synchronized(lock) { preparedMaterialIds.add(material.id) }
        if (!shouldCreate) return
        val bindGroup = material.createBindGroup(pipeline.shader) ?: return
        synchronized(lock) {
            pendingMaterialIds += material.id
            pendingMaterialBindGroups += bindGroup
            hasPendingBindGroups.value = true
        }
    }

    /** Adds a [Node3D] to be drawn on the next [flush] using the specified [Environment].. */
    fun render(node: Node3D, environment: Environment) {
        renderEnvironment = environment
        renderMaterial = null
        node.forEachMeshPrimitive(renderAction)
        renderEnvironment = null
    }

    /**
     * Adds a [Node3D] to be drawn on the next [flush] using the specified [Environment] with the
     * given material override.
     */
    fun render(node: Node3D, material: Material, environment: Environment) {
        renderEnvironment = environment
        renderMaterial = material
        node.forEachMeshPrimitive(renderAction)
        renderEnvironment = null
        renderMaterial = null
    }

    /**
     * Render any mesh primitives with the given node using the given [Camera] to attempt any
     * frustum culling.
     */
    fun render(node: Node3D, camera: Camera, environment: Environment) {
        renderEnvironment = environment
        renderMaterial = null
        node.forEachMeshPrimitive(camera, renderAction)
        renderEnvironment = null
    }

    /**
     * Render any mesh primitives with the given node using the given [Camera] to attempt any
     * frustum culling with an material override.
     */
    fun render(node: Node3D, camera: Camera, material: Material, environment: Environment) {
        renderEnvironment = environment
        renderMaterial = material
        node.forEachMeshPrimitive(camera, renderAction)
        renderEnvironment = null
        renderMaterial = null
    }

    /** Adds a [MeshPrimitive] to be drawn on the next [flush] using the specified [Environment]. */
    fun render(meshPrimitive: MeshPrimitive, environment: Environment) =
        queue(meshPrimitive, meshPrimitive.material, environment)

    /** Render a [MeshPrimitive] by overriding the attached material with the given material. */
    fun render(meshPrimitive: MeshPrimitive, material: Material, environment: Environment) =
        queue(meshPrimitive, material, environment)

    /**
     * Draws everything added since the last flush using the [camera]. This may be called multiple
     * times per frame, with different cameras and render passes.
     */
    fun flush(renderPassEncoder: RenderPassEncoder, camera: Camera, dt: Duration) {
        val frame = EngineStats.frame.value
        if (frame != lastFrame) {
            lastFrame = frame
            instanceOffset = 0
            instanceBuffers.releasePending()
        }
        if (pipelines.isEmpty()) {
            flushId = nextFlushId()
            return
        }
        sorter.sort(pipelines)

        var totalInstances = 0
        pipelines.fastForEach { pipeline ->
            pipeline.queue.fastForEach { totalInstances += it.visibleInstanceCount }
        }
        val instanceSize = MeshPrimitive.INSTANCE_DATA_SIZE
        instanceBuffers.ensureCapacity((instanceOffset + totalInstances) * instanceSize)
        if (instanceStaging.capacity < totalInstances * instanceSize) {
            instanceStaging =
                FloatBuffer(max(totalInstances * instanceSize, instanceStaging.capacity * 2))
        }

        var written = 0
        var drawCalls = 0
        var instanced = 0
        pipelines.fastForEach { pipeline ->
            val view = getEnvironmentView(pipeline.environment, camera, dt)
            renderPassEncoder.setPipeline(pipeline.renderPipeline)

            val environmentGroupIndex = pipeline.environmentGroupIndex()
            if (environmentGroupIndex >= 0) {
                renderPassEncoder.setBindGroup(
                    environmentGroupIndex,
                    pipeline.environmentBindGroup(view),
                )
            }
            val modelLayout = pipeline.modelLayout
            if (pipeline.modelGroupIndex >= 0 && modelLayout != null) {
                renderPassEncoder.setBindGroup(
                    pipeline.modelGroupIndex,
                    instanceBuffers.getOrCreateBindGroup(modelLayout),
                )
            }

            var lastMaterialId = -1
            val queue = pipeline.queue
            val materials = pipeline.queueMaterials
            for (i in 0 until queue.size) {
                val primitive = queue[i]
                if (primitive.drawnFlush == flushId && primitive.drawnPipeline === pipeline) {
                    continue
                }
                primitive.drawnFlush = flushId
                primitive.drawnPipeline = pipeline

                val count = primitive.writeVisibleInstanceData(instanceStaging, written * instanceSize)
                if (count <= 0) continue

                val material = materials[i]
                if (lastMaterialId != material.id) {
                    lastMaterialId = material.id
                    if (pipeline.materialGroupIndex >= 0) {
                        bindGroupByMaterialId[material.id]?.let {
                            renderPassEncoder.setBindGroup(pipeline.materialGroupIndex, it)
                        }
                    }
                    if (material.lastUpdateFrame != frame) {
                        material.lastUpdateFrame = frame
                        material.update()
                    }
                }

                val skin = primitive.skin
                if (material.skinned && skin != null && pipeline.skinGroupIndex >= 0) {
                    val skinBindGroup =
                        bindGroupBySkinId[skin.id] ?: error("Skin bind groups could not be found!")
                    renderPassEncoder.setBindGroup(pipeline.skinGroupIndex, skinBindGroup)
                    if (skin.lastUploadFrame != frame) {
                        skin.lastUploadFrame = frame
                        skin.writeToBuffer()
                    }
                }

                val firstInstance = instanceOffset + written
                written += count
                drawCalls++
                instanced += count - 1

                renderPassEncoder.setVertexBuffer(0, primitive.mesh.vbo)
                val indexedMesh = primitive.indexedMesh
                if (indexedMesh != null) {
                    renderPassEncoder.setIndexBuffer(
                        indexedMesh.ibo,
                        indexFormat = primitive.stripIndexFormat ?: IndexFormat.UINT16,
                    )
                    renderPassEncoder.drawIndexed(
                        indexedMesh.geometry.numIndices,
                        count,
                        firstInstance = firstInstance,
                    )
                } else {
                    renderPassEncoder.draw(
                        primitive.mesh.geometry.numVertices,
                        count,
                        firstInstance = firstInstance,
                    )
                }
            }
        }

        instanceBuffers.write(instanceStaging, instanceOffset * instanceSize, written * instanceSize)
        instanceOffset += written

        if (drawCalls > 0) {
            EngineStats.extra(DRAW_CALLS_STAT_NAME, drawCalls)
            EngineStats.extra(INSTANCED_STAT_NAME, instanced)
        }

        pipelines.fastForEach { pipeline ->
            pipeline.queue.fastForEach { it.resetVisibility() }
            pipeline.queue.clear()
            pipeline.queueMaterials.clear()
        }
        pipelines.clear()
        flushId = nextFlushId()
    }

    override fun release() {
        addPendingBindGroups()
        pipelineProviders.values.forEach { it.release() }
        bindGroupByMaterialId.forEachValue { it.release() }
        bindGroupByMaterialId.clear()
        bindGroupBySkinId.forEachValue { it.release() }
        bindGroupBySkinId.clear()
        instanceBuffers.release()
    }

    private fun queue(primitive: MeshPrimitive, material: Material, environment: Environment) {
        if (hasPendingBindGroups.value) {
            addPendingBindGroups()
        }
        val pipeline = getPipeline(primitive, material, environment)
        if (!material.ready) return
        getOrCreateMaterialBindGroup(material, pipeline)

        val skin = primitive.skin
        if (material.skinned && skin != null) {
            bindGroupBySkinId.getOrPut(skin.id) { skin.createBindGroup(pipeline.shader) }
        }

        if (pipeline.queuedFlush != flushId) {
            pipeline.queuedFlush = flushId
            pipeline.queue.clear()
            pipeline.queueMaterials.clear()
            pipelines += pipeline
        }
        if (primitive.queuedFlush == flushId && primitive.queuedPipeline === pipeline) return
        primitive.queuedFlush = flushId
        primitive.queuedPipeline = pipeline
        pipeline.queue += primitive
        pipeline.queueMaterials += material
    }

    private fun getPipeline(
        primitive: MeshPrimitive,
        material: Material,
        environment: Environment,
    ): MaterialPipeline {
        val cached = primitive.cachedPipeline
        if (
            cached != null &&
                primitive.cachedPipelineMaterial === material &&
                primitive.cachedPipelineEnvironment === environment &&
                primitive.cachedPipelineBatch === this &&
                primitive.cachedPipelineVersion == pipelineVersion &&
                primitive.cachedPipelineMaterialKey == material.key
        ) {
            return cached
        }
        val pipeline = lookupPipeline(primitive, material, environment)
        primitive.cachedPipeline = pipeline
        primitive.cachedPipelineMaterial = material
        primitive.cachedPipelineEnvironment = environment
        primitive.cachedPipelineBatch = this
        primitive.cachedPipelineVersion = pipelineVersion
        primitive.cachedPipelineMaterialKey = material.key
        return pipeline
    }

    private fun lookupPipeline(
        primitive: MeshPrimitive,
        material: Material,
        environment: Environment,
    ): MaterialPipeline =
        synchronized(lock) {
            pipelineProviders[material::class]?.getMaterialPipeline(
                device,
                material,
                environment,
                primitive.mesh.geometry.layout,
                primitive.topology,
                primitive.stripIndexFormat,
                colorFormat,
                depthFormat,
                sampleCount,
            )
        } ?: error("Unable to find pipeline for given instance!")

    private fun addPendingBindGroups() {
        synchronized(lock) {
            for (i in 0 until pendingMaterialIds.size) {
                val id = pendingMaterialIds[i]
                val bindGroup = pendingMaterialBindGroups[i]
                if (bindGroupByMaterialId[id] == null) {
                    bindGroupByMaterialId[id] = bindGroup
                } else {
                    bindGroup.release()
                }
            }
            pendingMaterialIds.clear()
            pendingMaterialBindGroups.clear()
            hasPendingBindGroups.value = false
        }
    }

    private fun getOrCreateMaterialBindGroup(material: Material, pipeline: MaterialPipeline) {
        if (bindGroupByMaterialId[material.id] != null) return
        material.createBindGroup(pipeline.shader)?.let { bindGroupByMaterialId[material.id] = it }
    }

    private fun getEnvironmentView(
        environment: Environment,
        camera: Camera,
        dt: Duration,
    ): CameraBuffersViaCamera {
        val view = environment.flushView
        if (environment.flushId == flushId && view != null) return view
        return environment.beginView(camera, dt).also {
            environment.flushId = flushId
            environment.flushView = it
        }
    }

    companion object {
        private const val INSTANCED_STAT_NAME = "ModelBatch instanced count"
        private const val DRAW_CALLS_STAT_NAME = "ModelBatch Draw calls"
        private const val INITIAL_INSTANCES = 256

        private var lastFlushId = 0L

        private fun nextFlushId(): Long = ++lastFlushId
    }
}
