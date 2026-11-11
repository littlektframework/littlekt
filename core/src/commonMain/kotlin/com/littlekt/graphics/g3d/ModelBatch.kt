package com.littlekt.graphics.g3d

import com.littlekt.EngineStats
import com.littlekt.Releasable
import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.Camera
import com.littlekt.graphics.g3d.material.Material
import com.littlekt.graphics.g3d.util.BaseMaterialPipelineProvider
import com.littlekt.graphics.g3d.util.BindGroupCache
import com.littlekt.graphics.g3d.util.InstanceBuffers
import com.littlekt.graphics.g3d.util.MaterialPipeline
import com.littlekt.graphics.g3d.util.MaterialPipelineProvider
import com.littlekt.graphics.g3d.util.MaterialPipelineSorter
import com.littlekt.graphics.util.CameraBuffersViaCamera
import com.littlekt.graphics.webgpu.*
import com.littlekt.util.datastructure.fastForEach
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.math.max
import kotlin.reflect.KClass
import kotlin.time.Duration

/**
 * A rendering helper class to handle pipeline caching and bind group caching for drawing 3D meshes.
 * A [ModelBatch] may be flushed multiple times within a single frame, such as for multiple passes
 * or cameras. Bind groups cached for a [Material] or skin are released once it is released.
 *
 * @author Colton Daily
 * @date 12/7/2024
 */
class ModelBatch(val device: Device) : Releasable {
    private val pipelineProviders = mutableMapOf<KClass<out Material>, MaterialPipelineProvider>()
    private val pipelines = mutableListOf<MaterialPipeline>()

    private val materialBindGroups = BindGroupCache<BindGroup>()
    private val skinBindGroups = BindGroupCache<BindGroup>()
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

    private val transparentDraws = mutableListOf<TransparentDraw>()
    private var boundPipeline: MaterialPipeline? = null
    private var boundMaterialId = -1
    private var written = 0
    private var drawCalls = 0
    private var instanced = 0

    private val lock = SynchronizedObject()

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

    /**
     * The [MaterialPipelineProvider] used for any [Material] type that doesn't have its own provider
     * added via [addPipelineProvider].
     */
    var defaultPipelineProvider: MaterialPipelineProvider? = null
        set(value) {
            if (field === value) return
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
    inline fun <reified T : Material> removePipelineProvider(): MaterialPipelineProvider? =
        removePipelineProvider(T::class)

    /** Removes a [MaterialPipelineProvider] of the given type. */
    fun <T : Material> removePipelineProvider(type: KClass<T>): MaterialPipelineProvider? =
        pipelineProviders.remove(type).also { pipelineVersion++ }

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
        val pipeline = lookupPipeline(meshPrimitive, material, environment) ?: return
        if (!material.ready || pipeline.materialGroupIndex < 0) return
        if (!materialBindGroups.markPrepared(material.id)) return
        val notifier = material.releaseNotifier
        val version = notifier.version
        val bindGroup = material.createBindGroup(pipeline.shader) ?: return
        materialBindGroups.addPending(material.id, notifier, version, bindGroup)
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

        written = 0
        drawCalls = 0
        instanced = 0
        boundPipeline = null
        var i = 0
        while (i < pipelines.size) {
            val pipeline = pipelines[i]
            if (pipeline.transparent) {
                var end = i + 1
                while (end < pipelines.size && pipelines[end].transparent) end++
                drawTransparent(renderPassEncoder, i, end, camera, dt, frame)
                i = end
                continue
            }
            bindPipeline(renderPassEncoder, pipeline, camera, dt)
            val queue = pipeline.queue
            val materials = pipeline.queueMaterials
            for (q in 0 until queue.size) {
                val primitive = queue[q]
                if (primitive.drawnFlush == flushId && primitive.drawnPipeline === pipeline) {
                    continue
                }
                primitive.drawnFlush = flushId
                primitive.drawnPipeline = pipeline

                val count = primitive.writeVisibleInstanceData(instanceStaging, written * instanceSize)
                if (count <= 0) continue
                drawPrimitive(renderPassEncoder, pipeline, primitive, materials[q], count, frame)
            }
            i++
        }

        instanceBuffers.write(instanceStaging, instanceOffset * instanceSize, written * instanceSize)
        instanceOffset += written
        boundPipeline = null

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
        materialBindGroups.sync()
        skinBindGroups.sync()
    }

    private fun drawTransparent(
        renderPassEncoder: RenderPassEncoder,
        start: Int,
        end: Int,
        camera: Camera,
        dt: Duration,
        frame: Long,
    ) {
        var count = 0
        val cameraPosition = camera.position
        for (p in start until end) {
            val pipeline = pipelines[p]
            val queue = pipeline.queue
            val materials = pipeline.queueMaterials
            for (q in 0 until queue.size) {
                val primitive = queue[q]
                if (primitive.drawnFlush == flushId && primitive.drawnPipeline === pipeline) {
                    continue
                }
                primitive.drawnFlush = flushId
                primitive.drawnPipeline = pipeline
                primitive.updateDirtyInstances()
                val material = materials[q]
                for (v in 0 until primitive.visibleInstanceCount) {
                    if (count == transparentDraws.size) transparentDraws += TransparentDraw()
                    val instance = primitive.visibleInstance(v)
                    val draw = transparentDraws[count++]
                    draw.pipeline = pipeline
                    draw.primitive = primitive
                    draw.material = material
                    draw.instance = instance
                    draw.distance = cameraPosition.sqrDistance(instance.globalBoundsSphere.center)
                }
            }
        }
        sortBackToFront(transparentDraws, 0, count - 1)

        val instanceSize = MeshPrimitive.INSTANCE_DATA_SIZE
        var k = 0
        while (k < count) {
            val draw = transparentDraws[k]
            val pipeline = draw.pipeline!!
            val primitive = draw.primitive!!
            val material = draw.material!!
            var runEnd = k
            while (
                runEnd < count &&
                    transparentDraws[runEnd].primitive === primitive &&
                    transparentDraws[runEnd].pipeline === pipeline &&
                    transparentDraws[runEnd].material === material
            ) {
                val runDraw = transparentDraws[runEnd]
                primitive.writeInstanceData(
                    runDraw.instance!!,
                    instanceStaging,
                    (written + runEnd - k) * instanceSize,
                )
                runDraw.clear()
                runEnd++
            }
            if (boundPipeline !== pipeline) bindPipeline(renderPassEncoder, pipeline, camera, dt)
            drawPrimitive(renderPassEncoder, pipeline, primitive, material, runEnd - k, frame)
            k = runEnd
        }
    }

    private fun bindPipeline(
        renderPassEncoder: RenderPassEncoder,
        pipeline: MaterialPipeline,
        camera: Camera,
        dt: Duration,
    ) {
        boundPipeline = pipeline
        boundMaterialId = -1
        val view = getEnvironmentView(pipeline.environment, camera, dt)
        renderPassEncoder.setPipeline(pipeline.renderPipeline)

        val environmentGroupIndex = pipeline.environmentGroupIndex()
        if (environmentGroupIndex >= 0) {
            renderPassEncoder.setBindGroup(environmentGroupIndex, pipeline.environmentBindGroup(view))
        }
        val modelLayout = pipeline.modelLayout
        if (pipeline.modelGroupIndex >= 0 && modelLayout != null) {
            renderPassEncoder.setBindGroup(
                pipeline.modelGroupIndex,
                instanceBuffers.getOrCreateBindGroup(modelLayout),
            )
        }
    }

    private fun drawPrimitive(
        renderPassEncoder: RenderPassEncoder,
        pipeline: MaterialPipeline,
        primitive: MeshPrimitive,
        material: Material,
        count: Int,
        frame: Long,
    ) {
        if (boundMaterialId != material.id) {
            boundMaterialId = material.id
            if (pipeline.materialGroupIndex >= 0) {
                materialBindGroups[material.id]?.let {
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
                skinBindGroups[skin.id] ?: error("Skin bind groups could not be found!")
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

    private fun sortBackToFront(draws: MutableList<TransparentDraw>, low: Int, high: Int) {
        var lo = low
        var hi = high
        while (lo < hi) {
            if (hi - lo < 16) {
                for (a in lo + 1..hi) {
                    val draw = draws[a]
                    var b = a - 1
                    while (b >= lo && draws[b].distance < draw.distance) {
                        draws[b + 1] = draws[b]
                        b--
                    }
                    draws[b + 1] = draw
                }
                return
            }
            val pivot = draws[(lo + hi) ushr 1].distance
            var left = lo
            var right = hi
            while (left <= right) {
                while (draws[left].distance > pivot) left++
                while (draws[right].distance < pivot) right--
                if (left <= right) {
                    val tmp = draws[left]
                    draws[left] = draws[right]
                    draws[right] = tmp
                    left++
                    right--
                }
            }
            if (right - lo < hi - left) {
                sortBackToFront(draws, lo, right)
                lo = left
            } else {
                sortBackToFront(draws, left, hi)
                hi = right
            }
        }
    }

    override fun release() {
        pipelineProviders.values.forEach { it.release() }
        defaultPipelineProvider?.release()
        materialBindGroups.release()
        skinBindGroups.release()
        instanceBuffers.release()
    }

    private fun queue(primitive: MeshPrimitive, material: Material, environment: Environment) {
        materialBindGroups.sync()
        skinBindGroups.sync()
        val pipeline = getPipeline(primitive, material, environment) ?: return
        if (!material.ready) return
        if (pipeline.materialGroupIndex >= 0) {
            getOrCreateMaterialBindGroup(material, pipeline)
        }

        val skin = primitive.skin
        if (
            material.skinned &&
                skin != null &&
                pipeline.skinGroupIndex >= 0 &&
                skinBindGroups[skin.id] == null
        ) {
            val notifier = skin.releaseNotifier
            val version = notifier.version
            skinBindGroups.put(skin.id, notifier, version, skin.createBindGroup(pipeline.shader))
                ?: return
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
    ): MaterialPipeline? {
        val cache = primitive.pipelineCache
        val materialKey = material.key
        for (i in cache.indices) {
            val entry = cache[i]
            if (
                entry.batch === this &&
                    entry.material === material &&
                    entry.environment === environment &&
                    entry.version == pipelineVersion &&
                    entry.materialKey == materialKey
            ) {
                return entry.pipeline
            }
        }
        val pipeline = lookupPipeline(primitive, material, environment)
        val entry = cache[primitive.pipelineCacheNext]
        primitive.pipelineCacheNext = (primitive.pipelineCacheNext + 1) % cache.size
        entry.pipeline = pipeline
        entry.material = material
        entry.environment = environment
        entry.batch = this
        entry.version = pipelineVersion
        entry.materialKey = materialKey
        return pipeline
    }

    private fun lookupPipeline(
        primitive: MeshPrimitive,
        material: Material,
        environment: Environment,
    ): MaterialPipeline? =
        synchronized(lock) {
            val provider =
                pipelineProviders[material::class]
                    ?: defaultPipelineProvider
                    ?: error("No MaterialPipelineProvider found for ${material::class}!")
            provider.getMaterialPipeline(
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
        }

    private fun getOrCreateMaterialBindGroup(material: Material, pipeline: MaterialPipeline) {
        if (materialBindGroups[material.id] != null) return
        val notifier = material.releaseNotifier
        val version = notifier.version
        val bindGroup = material.createBindGroup(pipeline.shader) ?: return
        materialBindGroups.put(material.id, notifier, version, bindGroup)
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

private class TransparentDraw {
    var pipeline: MaterialPipeline? = null
    var primitive: MeshPrimitive? = null
    var material: Material? = null
    var instance: MeshPrimitiveInstance? = null
    var distance = 0f

    fun clear() {
        pipeline = null
        primitive = null
        material = null
        instance = null
    }
}
