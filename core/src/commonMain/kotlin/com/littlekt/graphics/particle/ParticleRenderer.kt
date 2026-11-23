package com.littlekt.graphics.particle

import com.littlekt.EngineStats
import com.littlekt.Releasable
import com.littlekt.file.ByteBuffer
import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.Camera
import com.littlekt.graphics.EmptyTexture
import com.littlekt.graphics.Texture
import com.littlekt.graphics.webgpu.*
import com.littlekt.log.Logger
import com.littlekt.util.align
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Simulates [ParticleEmitter]s with a compute pass and draws them as camera facing quads. Set
 * [colorFormat], [depthFormat] & [sampleCount] to match the render pass, like a
 * [com.littlekt.graphics.g3d.ModelBatch].
 *
 * Works in 2D with an orthographic camera and particles on the `z = 0` plane, such as through a
 * scene graph `GpuParticles2D` node. Set [depthFormat] to `null` when the pass has no depth target.
 *
 * Particles are tested against the depth buffer without writing to it, so draw them after opaque
 * geometry. [ParticleBlend.OPAQUE] particles write depth. [ParticleEffect.mesh] particles are drawn
 * as lit 3D shapes, shaded with [light]. [ParticleBlend.DISTORT] particles are drawn into a
 * [com.littlekt.graphics.postprocess.DistortionEffect] pass instead of the scene.
 *
 * ```
 * renderFrame { frame, commandEncoder ->
 *     particles.simulate(commandEncoder, emitter)
 *     val renderPass = commandEncoder.beginRenderPass(...)
 *     modelBatch.flush(renderPass, camera, dt)
 *     particles.render(renderPass, camera, emitter)
 *     renderPass.end()
 * }
 * ```
 *
 * @author Colton Daily
 * @date 11/14/2026
 */
class ParticleRenderer(val device: Device) : Releasable {

    /** The format of the color target particles are drawn to. */
    var colorFormat: TextureFormat = TextureFormat.BGRA8_UNORM_SRGB

    /** The format of the depth target, or `null` if the render pass has none. */
    var depthFormat: TextureFormat? = TextureFormat.DEPTH24_PLUS_STENCIL8

    /** The format of the target [ParticleBlend.DISTORT] particles are drawn to. */
    var distortionFormat: TextureFormat = TextureFormat.RGBA16_FLOAT

    /** The light [ParticleEffect.mesh] particles are shaded with. */
    val light: ParticleLight = ParticleLight()

    /** The number of samples of the render pass targets, `1` or `4`. */
    var sampleCount: Int = 1
        set(value) {
            require(value == 1 || value == 4) { "ParticleRenderer.sampleCount must be 1 or 4!" }
            field = value
        }

    /** The most cameras particles can be drawn with in a single frame. */
    var maxViewsPerFrame: Int = 8
        set(value) {
            require(value > 0) { "maxViewsPerFrame must be greater than 0!" }
            if (value == field) return
            field = value
            recreateCameraBuffer()
        }

    internal val computeLayout =
        device.createBindGroupLayout(
            BindGroupLayoutDescriptor(
                listOf(
                    BindGroupLayoutEntry(0, ShaderStage.COMPUTE, BufferBindingLayout()),
                    BindGroupLayoutEntry(
                        1,
                        ShaderStage.COMPUTE,
                        BufferBindingLayout(BufferBindingType.STORAGE),
                    ),
                    BindGroupLayoutEntry(
                        2,
                        ShaderStage.COMPUTE,
                        BufferBindingLayout(BufferBindingType.STORAGE),
                    ),
                    BindGroupLayoutEntry(
                        3,
                        ShaderStage.COMPUTE,
                        BufferBindingLayout(BufferBindingType.STORAGE),
                    ),
                ),
                "ParticleRenderer compute layout",
            )
        )

    internal val emitterLayout =
        device.createBindGroupLayout(
            BindGroupLayoutDescriptor(
                listOf(
                    BindGroupLayoutEntry(
                        0,
                        ShaderStage.VERTEX or ShaderStage.FRAGMENT,
                        BufferBindingLayout(),
                    ),
                    BindGroupLayoutEntry(
                        1,
                        ShaderStage.VERTEX,
                        BufferBindingLayout(BufferBindingType.READ_ONLY_STORAGE),
                    ),
                    BindGroupLayoutEntry(
                        2,
                        ShaderStage.VERTEX,
                        BufferBindingLayout(BufferBindingType.READ_ONLY_STORAGE),
                    ),
                ),
                "ParticleRenderer emitter layout",
            )
        )

    internal val sortLayout =
        device.createBindGroupLayout(
            BindGroupLayoutDescriptor(
                listOf(
                    BindGroupLayoutEntry(
                        0,
                        ShaderStage.COMPUTE,
                        BufferBindingLayout(BufferBindingType.STORAGE),
                    ),
                    BindGroupLayoutEntry(
                        1,
                        ShaderStage.COMPUTE,
                        BufferBindingLayout(BufferBindingType.STORAGE),
                    ),
                    BindGroupLayoutEntry(2, ShaderStage.COMPUTE, BufferBindingLayout()),
                ),
                "ParticleRenderer sort layout",
            )
        )

    internal val eventsLayout =
        device.createBindGroupLayout(
            BindGroupLayoutDescriptor(
                listOf(
                    BindGroupLayoutEntry(
                        0,
                        ShaderStage.COMPUTE,
                        BufferBindingLayout(BufferBindingType.STORAGE),
                    ),
                    BindGroupLayoutEntry(
                        1,
                        ShaderStage.COMPUTE,
                        BufferBindingLayout(BufferBindingType.STORAGE),
                    ),
                    BindGroupLayoutEntry(
                        2,
                        ShaderStage.COMPUTE,
                        BufferBindingLayout(BufferBindingType.STORAGE),
                    ),
                    BindGroupLayoutEntry(3, ShaderStage.COMPUTE, BufferBindingLayout()),
                ),
                "ParticleRenderer events layout",
            )
        )

    private val stepStride = 16.align(device.limits.minUniformBufferOffsetAlignment)

    private val stepLayout =
        device.createBindGroupLayout(
            BindGroupLayoutDescriptor(
                listOf(
                    BindGroupLayoutEntry(
                        0,
                        ShaderStage.COMPUTE,
                        BufferBindingLayout(hasDynamicOffset = true, minBindingSize = 16L),
                    )
                ),
                "ParticleRenderer sort step layout",
            )
        )

    private val cameraStride =
        (CAMERA_FLOATS * Float.SIZE_BYTES).align(device.limits.minUniformBufferOffsetAlignment)

    private val cameraLayout =
        device.createBindGroupLayout(
            BindGroupLayoutDescriptor(
                listOf(
                    BindGroupLayoutEntry(
                        0,
                        ShaderStage.VERTEX or ShaderStage.FRAGMENT,
                        BufferBindingLayout(
                            hasDynamicOffset = true,
                            minBindingSize = CAMERA_FLOATS * Float.SIZE_BYTES.toLong(),
                        ),
                    )
                ),
                "ParticleRenderer camera layout",
            )
        )

    private val textureLayout =
        device.createBindGroupLayout(
            BindGroupLayoutDescriptor(
                listOf(
                    BindGroupLayoutEntry(0, ShaderStage.FRAGMENT, TextureBindingLayout()),
                    BindGroupLayoutEntry(1, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                ),
                "ParticleRenderer texture layout",
            )
        )

    private val meshLayout =
        device.createBindGroupLayout(
            BindGroupLayoutDescriptor(
                listOf(
                    BindGroupLayoutEntry(
                        0,
                        ShaderStage.VERTEX,
                        BufferBindingLayout(BufferBindingType.READ_ONLY_STORAGE),
                    ),
                    BindGroupLayoutEntry(1, ShaderStage.FRAGMENT, BufferBindingLayout()),
                ),
                "ParticleRenderer mesh layout",
            )
        )

    private val lightData = FloatBuffer(LIGHT_FLOATS)
    private val lightBuffer =
        device.createBuffer(
            BufferDescriptor(
                "ParticleRenderer light",
                LIGHT_FLOATS * Float.SIZE_BYTES.toLong(),
                BufferUsage.UNIFORM or BufferUsage.COPY_DST,
                false,
            )
        )
    private var lightFrame = -1L

    private val depthLayout = createDepthLayout(multisampled = false)
    private val msaaDepthLayout = createDepthLayout(multisampled = true)

    private val computeShader = device.createShaderModule(COMPUTE_WGSL)
    private val sortShader = device.createShaderModule(SORT_WGSL)
    private val renderShader = device.createShaderModule(RENDER_WGSL)
    private val meshShader = device.createShaderModule(RENDER_MESH_WGSL)
    private val meshPipelineLayout =
        device.createPipelineLayout(
            PipelineLayoutDescriptor(
                listOf(cameraLayout, emitterLayout, textureLayout, meshLayout),
                "ParticleRenderer mesh pipeline",
            )
        )
    private val msaaSoftShader =
        device.createShaderModule(
            RENDER_WGSL.replace("texture_depth_2d", "texture_depth_multisampled_2d")
        )
    private val computePipelineLayout =
        device.createPipelineLayout(
            PipelineLayoutDescriptor(listOf(computeLayout), "ParticleRenderer compute pipeline")
        )
    private val renderPipelineLayout =
        device.createPipelineLayout(
            PipelineLayoutDescriptor(
                listOf(cameraLayout, emitterLayout, textureLayout),
                "ParticleRenderer render pipeline",
            )
        )
    private val softPipelineLayout =
        device.createPipelineLayout(
            PipelineLayoutDescriptor(
                listOf(cameraLayout, emitterLayout, textureLayout, depthLayout),
                "ParticleRenderer soft render pipeline",
            )
        )
    private val softMsaaPipelineLayout =
        device.createPipelineLayout(
            PipelineLayoutDescriptor(
                listOf(cameraLayout, emitterLayout, textureLayout, msaaDepthLayout),
                "ParticleRenderer soft msaa render pipeline",
            )
        )
    private var depthBindGroup: BindGroup? = null
    private var depthBindGroupId = -1
    private val sortPipelineLayout =
        device.createPipelineLayout(
            PipelineLayoutDescriptor(
                listOf(computeLayout, sortLayout, stepLayout),
                "ParticleRenderer sort pipeline",
            )
        )
    private val spawnPipelineLayout =
        device.createPipelineLayout(
            PipelineLayoutDescriptor(
                listOf(computeLayout, eventsLayout),
                "ParticleRenderer spawn pipeline",
            )
        )
    private val sortKeysPipeline =
        device.createComputePipeline(
            ComputePipelineDescriptor(
                sortPipelineLayout,
                ProgrammableStage(sortShader, "sort_keys"),
                "ParticleRenderer sort keys",
            )
        )
    private val sortStepPipeline =
        device.createComputePipeline(
            ComputePipelineDescriptor(
                sortPipelineLayout,
                ProgrammableStage(sortShader, "sort_step"),
                "ParticleRenderer sort step",
            )
        )
    private val spawnPipeline =
        device.createComputePipeline(
            ComputePipelineDescriptor(
                spawnPipelineLayout,
                ProgrammableStage(computeShader, "spawn_events"),
                "ParticleRenderer spawn events",
            )
        )
    private val stepBuffer = createStepBuffer()
    private val stepBindGroup =
        device.createBindGroup(
            BindGroupDescriptor(
                stepLayout,
                listOf(BindGroupEntry(0, BufferBinding(stepBuffer, 0, 16L))),
                label = "ParticleRenderer sort step bind group",
            )
        )
    private val singleEmitter = ArrayList<ParticleEmitter>(1)
    private val computePipeline =
        device.createComputePipeline(
            ComputePipelineDescriptor(
                computePipelineLayout,
                ProgrammableStage(computeShader, "simulate"),
                "ParticleRenderer simulate",
            )
        )
    private val pipelines = mutableListOf<PipelineEntry>()

    /** The soft round dot used when [ParticleEffect.texture] is `null`. */
    val defaultTexture: Texture = createDefaultTexture()

    private var cameraBuffer: GPUBuffer = createCameraBuffer()
    private var cameraBindGroup: BindGroup = createCameraBindGroup()
    private val cameraData = FloatBuffer(CAMERA_FLOATS)
    private val cameraViews = mutableListOf<FloatArray>()
    private val cameraRefs = mutableListOf<Camera?>()
    private var cameraSizes = LongArray(4)
    private var viewFrame = -1L
    private var viewCount = 0

    /** `true` once released. */
    var isReleased: Boolean = false
        private set

    /**
     * Spawns & moves the particles of [emitter], and spawns its [ParticleEmitter.subEmitter]
     * particles. Call once per frame after the emitter is updated and before the render pass that
     * draws it.
     *
     * @param camera the camera that [ParticleEffect.sorted] emitters are sorted back to front for,
     *   or `null` to skip sorting
     */
    fun simulate(commandEncoder: CommandEncoder, emitter: ParticleEmitter, camera: Camera? = null) {
        singleEmitter.clear()
        singleEmitter += emitter
        simulate(commandEncoder, singleEmitter, camera)
        singleEmitter.clear()
    }

    /** Simulates each of [emitters] in one compute pass. @see simulate */
    fun simulate(
        commandEncoder: CommandEncoder,
        emitters: List<ParticleEmitter>,
        camera: Camera? = null,
    ) {
        var pass: ComputePassEncoder? = null
        for (i in emitters.indices) {
            val emitter = emitters[i]
            val slots = emitter.prepare()
            if (slots > 0) {
                val encoder =
                    pass
                        ?: commandEncoder.beginComputePass("ParticleRenderer simulate").also {
                            pass = it
                        }
                encoder.setPipeline(computePipeline)
                encoder.setBindGroup(0, emitter.computeBindGroup)
                encoder.dispatchWorkgroups(groups(slots))
                val sub = emitter.subEmitter
                val events = emitter.eventsBindGroup
                if (sub != null && events != null) {
                    encoder.setPipeline(spawnPipeline)
                    encoder.setBindGroup(0, sub.emitter.computeBindGroup)
                    encoder.setBindGroup(1, events)
                    encoder.dispatchWorkgroups(groups(emitter.eventSpawnThreads))
                }
            }
            if (camera != null) {
                val size = emitter.prepareSort(camera)
                val sortGroup = emitter.sortBindGroup
                if (size > 1 && sortGroup != null) {
                    val encoder =
                        pass
                            ?: commandEncoder.beginComputePass("ParticleRenderer simulate").also {
                                pass = it
                            }
                    sort(encoder, emitter, sortGroup, size)
                }
            }
        }
        pass?.let {
            it.end()
            it.release()
        }
    }

    private fun sort(encoder: ComputePassEncoder, emitter: ParticleEmitter, group: BindGroup, size: Int) {
        val workgroups = groups(size)
        encoder.setPipeline(sortKeysPipeline)
        encoder.setBindGroup(0, emitter.computeBindGroup)
        encoder.setBindGroup(1, group)
        encoder.setBindGroup(2, stepBindGroup, 0)
        encoder.dispatchWorkgroups(workgroups)
        encoder.setPipeline(sortStepPipeline)
        var log = 0
        while ((1 shl log) < size) log++
        val steps = log * (log + 1) / 2
        for (step in 0 until steps) {
            encoder.setBindGroup(2, stepBindGroup, step * stepStride)
            encoder.dispatchWorkgroups(workgroups)
        }
    }

    private fun groups(threads: Int): Int = (threads + WORKGROUP_SIZE - 1) / WORKGROUP_SIZE

    private fun createStepBuffer(): GPUBuffer {
        val floatsPerStep = stepStride / Float.SIZE_BYTES
        val steps = sortSteps(MAX_SORT_LOG2)
        val total = steps.size / 2
        val data = FloatBuffer(total * floatsPerStep)
        for (index in 0 until total) {
            data[index * floatsPerStep] = steps[index * 2].toFloat()
            data[index * floatsPerStep + 1] = steps[index * 2 + 1].toFloat()
        }
        val buffer =
            device.createBuffer(
                BufferDescriptor(
                    "ParticleRenderer sort steps",
                    total.toLong() * stepStride,
                    BufferUsage.UNIFORM or BufferUsage.COPY_DST,
                    false,
                )
            )
        device.queue.writeBuffer(buffer, data)
        return buffer
    }

    /**
     * Draws the particles of [emitter] with [camera] within [renderPassEncoder].
     *
     * @param depth the depth texture of the opaque geometry, attached to the pass with
     *   [EmptyTexture.readOnlyDepthAttachment], so particles with a [ParticleEffect.softness] fade
     *   where they meet it. `null` draws every particle hard. [ParticleBlend.DISTORT] particles use
     *   it to hide behind geometry, without it being attached.
     */
    fun render(
        renderPassEncoder: RenderPassEncoder,
        camera: Camera,
        emitter: ParticleEmitter,
        depth: EmptyTexture? = null,
    ) {
        val count = emitter.activeCount
        if (count == 0) return
        val effect = emitter.effect
        val mesh = effect.mesh
        if (mesh != null) {
            writeLight()
            val offset = cameraOffset(camera, depth?.width ?: 0, depth?.height ?: 0)
            renderPassEncoder.setPipeline(pipeline(effect.blend, soft = false, msaaDepth = false, mesh = true))
            renderPassEncoder.setBindGroup(0, cameraBindGroup, offset)
            renderPassEncoder.setBindGroup(1, emitter.renderBindGroup)
            renderPassEncoder.setBindGroup(2, emitter.textureBindGroup)
            renderPassEncoder.setBindGroup(3, emitter.meshBindGroup!!)
            renderPassEncoder.draw(mesh.vertexCount, count)
            return
        }
        val soft = depth != null && effect.blend != ParticleBlend.OPAQUE && (effect.softness > 0f || effect.blend == ParticleBlend.DISTORT)
        val msaaDepth = soft && depth != null && depth.textureDescriptor.sampleCount > 1
        val offset = cameraOffset(camera, depth?.width ?: 0, depth?.height ?: 0)
        renderPassEncoder.setPipeline(pipeline(effect.blend, soft, msaaDepth, mesh = false))
        renderPassEncoder.setBindGroup(0, cameraBindGroup, offset)
        renderPassEncoder.setBindGroup(1, emitter.renderBindGroup)
        renderPassEncoder.setBindGroup(2, emitter.textureBindGroup)
        if (soft && depth != null) renderPassEncoder.setBindGroup(3, depthBindGroup(depth, msaaDepth))
        renderPassEncoder.draw(6, count)
    }

    /** Draws each of [emitters] with [camera] within [renderPassEncoder]. @see render */
    fun render(
        renderPassEncoder: RenderPassEncoder,
        camera: Camera,
        emitters: List<ParticleEmitter>,
        depth: EmptyTexture? = null,
    ) {
        for (i in emitters.indices) render(renderPassEncoder, camera, emitters[i], depth)
    }

    private fun depthBindGroup(depth: EmptyTexture, multisampled: Boolean): BindGroup {
        val current = depthBindGroup
        if (current != null && depthBindGroupId == depth.id) return current
        current?.release()
        depthBindGroupId = depth.id
        return device
            .createBindGroup(
                BindGroupDescriptor(
                    if (multisampled) msaaDepthLayout else depthLayout,
                    listOf(BindGroupEntry(0, depth.depthView)),
                    label = "ParticleRenderer depth bind group",
                )
            )
            .also { depthBindGroup = it }
    }

    internal fun createMeshBindGroup(mesh: ParticleMesh): BindGroup =
        device.createBindGroup(
            BindGroupDescriptor(
                meshLayout,
                listOf(
                    BindGroupEntry(0, BufferBinding(mesh.buffer)),
                    BindGroupEntry(1, BufferBinding(lightBuffer)),
                ),
                label = "ParticleRenderer mesh bind group",
            )
        )

    private fun writeLight() {
        val frame = EngineStats.frame.value
        if (frame == lightFrame) return
        lightFrame = frame
        val d = light.direction
        val length = sqrt(d.x * d.x + d.y * d.y + d.z * d.z).let { if (it > 0f) it else 1f }
        lightData[0] = d.x / length
        lightData[1] = d.y / length
        lightData[2] = d.z / length
        lightData[3] = 0f
        lightData[4] = light.color.r
        lightData[5] = light.color.g
        lightData[6] = light.color.b
        lightData[7] = 1f
        lightData[8] = light.ambient.r
        lightData[9] = light.ambient.g
        lightData[10] = light.ambient.b
        lightData[11] = 1f
        device.queue.writeBuffer(lightBuffer, lightData)
    }

    internal fun createTextureBindGroup(texture: Texture?): BindGroup {
        val source = texture ?: defaultTexture
        return device.createBindGroup(
            BindGroupDescriptor(
                textureLayout,
                listOf(BindGroupEntry(0, source.view), BindGroupEntry(1, source.sampler)),
                label = "ParticleRenderer texture bind group",
            )
        )
    }

    private fun cameraOffset(camera: Camera, width: Int, height: Int): Int {
        val frame = EngineStats.frame.value
        if (frame != viewFrame) {
            viewFrame = frame
            viewCount = 0
        }
        val viewProjection = camera.viewProjection.data
        val size = (width.toLong() shl 32) or (height.toLong() and 0xFFFFFFFFL)
        for (i in 0 until viewCount) {
            if (
                cameraRefs[i] === camera &&
                    cameraSizes[i] == size &&
                    cameraViews[i].contentEquals(viewProjection)
            ) {
                return i * cameraStride
            }
        }
        if (viewCount >= maxViewsPerFrame) {
            logger.warn {
                "ParticleRenderer has exceeded maxViewsPerFrame ($maxViewsPerFrame). Reusing the last view which will overwrite its data!"
            }
            viewCount = maxViewsPerFrame - 1
        }
        val index = viewCount++
        while (cameraViews.size <= index) {
            cameraViews += FloatArray(16)
            cameraRefs += null
            if (cameraSizes.size <= index) cameraSizes = cameraSizes.copyOf(index * 2 + 1)
        }
        viewProjection.copyInto(cameraViews[index])
        cameraRefs[index] = camera
        cameraSizes[index] = size
        writeCamera(camera, index, width, height)
        return index * cameraStride
    }

    private fun writeCamera(camera: Camera, index: Int, width: Int, height: Int) {
        val viewProjection = camera.viewProjection.data
        for (i in 0 until 16) cameraData[i] = viewProjection[i]
        val view = camera.view.data
        setNormalized(16, view[0], view[4], view[8])
        setNormalized(20, view[1], view[5], view[9])
        setNormalized(24, view[2], view[6], view[10])
        val inverseProjection = camera.invProj.data
        for (i in 0 until 16) cameraData[28 + i] = inverseProjection[i]
        cameraData[44] = width.toFloat()
        cameraData[45] = height.toFloat()
        cameraData[46] = 0f
        cameraData[47] = 0f
        device.queue.writeBuffer(cameraBuffer, cameraData, offset = index.toLong() * cameraStride)
    }

    private fun setNormalized(offset: Int, x: Float, y: Float, z: Float) {
        val length = sqrt(x * x + y * y + z * z).let { if (it > 0f) it else 1f }
        cameraData[offset] = x / length
        cameraData[offset + 1] = y / length
        cameraData[offset + 2] = z / length
        cameraData[offset + 3] = 0f
    }

    private fun pipeline(blend: ParticleBlend, soft: Boolean, msaaDepth: Boolean, mesh: Boolean): RenderPipeline {
        val distort = blend == ParticleBlend.DISTORT
        val depth = if (distort) null else depthFormat
        val format = if (distort) distortionFormat else colorFormat
        val samples = if (distort) 1 else sampleCount
        for (i in pipelines.indices) {
            val entry = pipelines[i]
            if (
                entry.blend == blend &&
                entry.soft == soft &&
                entry.msaaDepth == msaaDepth &&
                entry.mesh == mesh &&
                entry.colorFormat == format &&
                entry.depthFormat == depth &&
                entry.sampleCount == samples
            ) {
                return entry.pipeline
            }
        }
        val shader =
            when {
                mesh -> meshShader
                msaaDepth -> msaaSoftShader
                else -> renderShader
            }
        val fragmentEntry =
            when {
                mesh -> if (blend == ParticleBlend.OPAQUE) "fs_mesh_opaque" else "fs_mesh"
                distort -> if (soft) "fs_distort_soft" else "fs_distort"
                blend == ParticleBlend.OPAQUE -> "fs_opaque"
                soft -> "fs_soft"
                else -> "fs_main"
            }
        val pipeline =
            device.createRenderPipeline(
                RenderPipelineDescriptor(
                    layout =
                        when {
                            mesh -> meshPipelineLayout
                            msaaDepth -> softMsaaPipelineLayout
                            soft -> softPipelineLayout
                            else -> renderPipelineLayout
                        },
                    vertex = VertexState(shader, if (mesh) "vs_mesh" else "vs_main"),
                    fragment =
                        FragmentState(
                            shader,
                            fragmentEntry,
                            listOf(
                                ColorTargetState(
                                    format,
                                    when (blend) {
                                        ParticleBlend.ALPHA -> BlendState.NonPreMultiplied
                                        ParticleBlend.ADDITIVE -> BlendState.Add
                                        ParticleBlend.OPAQUE -> null
                                        ParticleBlend.DISTORT -> BlendState.Add
                                    },
                                )
                            ),
                        ),
                    depthStencil =
                        depth?.let {
                            DepthStencilState(
                                format = it,
                                depthWriteEnabled = blend == ParticleBlend.OPAQUE,
                                depthCompare = CompareFunction.LESS_EQUAL,
                            )
                        },
                    multisample = MultisampleState(count = samples),
                    label = "ParticleRenderer $blend${if (mesh) " mesh" else ""} pipeline",
                )
            )
        pipelines += PipelineEntry(blend, soft, msaaDepth, mesh, format, depth, samples, pipeline)
        return pipeline
    }

    private fun createDefaultTexture(): Texture {
        val size = DEFAULT_TEXTURE_SIZE
        val pixels = ByteArray(size * size * 4)
        val half = size / 2f
        for (y in 0 until size) {
            for (x in 0 until size) {
                val dx = (x + 0.5f - half) / half
                val dy = (y + 0.5f - half) / half
                val falloff = (1f - sqrt(dx * dx + dy * dy)).coerceIn(0f, 1f)
                val i = (y * size + x) * 4
                pixels[i] = WHITE
                pixels[i + 1] = WHITE
                pixels[i + 2] = WHITE
                pixels[i + 3] = (falloff * falloff * 255f).toInt().toByte()
            }
        }
        val texture =
            EmptyTexture(
                device,
                TextureFormat.RGBA8_UNORM,
                size,
                size,
                SamplerDescriptor(magFilter = FilterMode.LINEAR, minFilter = FilterMode.LINEAR),
                usage = TextureUsage.TEXTURE or TextureUsage.COPY_DST,
            )
        device.queue.writeTexture(
            ByteBuffer(pixels),
            TextureCopyView(texture.gpuTexture),
            TextureDataLayout(size * 4, size),
            Extent3D(size, size, 1),
        )
        return texture
    }

    private fun createCameraBuffer(): GPUBuffer =
        device.createBuffer(
            BufferDescriptor(
                "ParticleRenderer camera",
                maxViewsPerFrame.toLong() * cameraStride,
                BufferUsage.UNIFORM or BufferUsage.COPY_DST,
                false,
            )
        )

    private fun createCameraBindGroup(): BindGroup =
        device.createBindGroup(
            BindGroupDescriptor(
                cameraLayout,
                listOf(
                    BindGroupEntry(
                        0,
                        BufferBinding(cameraBuffer, 0, CAMERA_FLOATS * Float.SIZE_BYTES.toLong()),
                    )
                ),
                label = "ParticleRenderer camera bind group",
            )
        )

    private fun recreateCameraBuffer() {
        cameraBindGroup.release()
        cameraBuffer.release()
        cameraBuffer = createCameraBuffer()
        cameraBindGroup = createCameraBindGroup()
        viewFrame = -1L
    }

    override fun release() {
        if (isReleased) return
        isReleased = true
        pipelines.forEach { it.pipeline.release() }
        pipelines.clear()
        cameraBindGroup.release()
        cameraBuffer.release()
        defaultTexture.release()
        computePipeline.release()
        sortKeysPipeline.release()
        sortStepPipeline.release()
        spawnPipeline.release()
        sortPipelineLayout.release()
        spawnPipelineLayout.release()
        stepBindGroup.release()
        stepBuffer.release()
        sortShader.release()
        sortLayout.release()
        eventsLayout.release()
        stepLayout.release()
        computePipelineLayout.release()
        renderPipelineLayout.release()
        softPipelineLayout.release()
        softMsaaPipelineLayout.release()
        msaaSoftShader.release()
        msaaDepthLayout.release()
        depthBindGroup?.release()
        depthLayout.release()
        computeShader.release()
        renderShader.release()
        meshShader.release()
        meshPipelineLayout.release()
        meshLayout.release()
        lightBuffer.release()
        computeLayout.release()
        emitterLayout.release()
        cameraLayout.release()
        textureLayout.release()
    }

    private fun createDepthLayout(multisampled: Boolean): BindGroupLayout =
        device.createBindGroupLayout(
            BindGroupLayoutDescriptor(
                listOf(
                    BindGroupLayoutEntry(
                        0,
                        ShaderStage.FRAGMENT,
                        TextureBindingLayout(
                            sampleType = TextureSampleType.DEPTH,
                            multisampled = multisampled,
                        ),
                    )
                ),
                "ParticleRenderer depth layout",
            )
        )

    private class PipelineEntry(
        val blend: ParticleBlend,
        val soft: Boolean,
        val msaaDepth: Boolean,
        val mesh: Boolean,
        val colorFormat: TextureFormat,
        val depthFormat: TextureFormat?,
        val sampleCount: Int,
        val pipeline: RenderPipeline,
    )

    internal companion object {
        const val CAMERA_FLOATS = 48
        const val LIGHT_FLOATS = 12
        const val WORKGROUP_SIZE = 64
        const val MAX_SORT_LOG2 = 24

        /**
         * @return the `k` & `j` pairs of a bitonic sort of up to `2^maxLog2` values. Sorting `2^n`
         *   values uses the first `n * (n + 1) / 2` pairs.
         */
        fun sortSteps(maxLog2: Int): IntArray {
            val steps = IntArray(maxLog2 * (maxLog2 + 1))
            var index = 0
            var k = 2
            while (k <= 1 shl maxLog2) {
                var j = k / 2
                while (j > 0) {
                    steps[index++] = k
                    steps[index++] = j
                    j /= 2
                }
                k *= 2
            }
            return steps
        }
        const val DEFAULT_TEXTURE_SIZE = 64
        const val WHITE = 0xFF.toByte()
        val logger = Logger<ParticleRenderer>()

        const val PARAMS_WGSL =
            """
struct Params {
    transform: mat4x4f,
    previous: vec4f,
    spawn: vec4f,
    gravity: vec4f,
    direction: vec4f,
    shape: vec4f,
    life_speed: vec4f,
    size_radial: vec4f,
    rotation: vec4f,
    flags: vec4f,
    sizes: vec4f,
    colors: array<vec4f, 4>,
    extra: vec4f,
    plane: vec4f,
    sheet: vec4f,
    sheet_flags: vec4f,
    mesh: vec4f,
};

struct Particle {
    position: vec4f,
    velocity: vec4f,
    misc: vec4f,
};

struct Event {
    position: vec4f,
    velocity: vec4f,
};
"""

        const val COMPUTE_WGSL =
            PARAMS_WGSL +
                    """
@group(0) @binding(0) var<uniform> params: Params;
@group(0) @binding(1) var<storage, read_write> particles: array<Particle>;
@group(0) @binding(2) var<storage, read_write> events: array<Event>;
@group(0) @binding(3) var<storage, read_write> event_count: atomic<u32>;
@group(1) @binding(0) var<storage, read_write> source_events: array<Event>;
@group(1) @binding(1) var<storage, read_write> source_count: atomic<u32>;
@group(1) @binding(2) var<storage, read_write> spawn_cursor: atomic<u32>;
@group(1) @binding(3) var<uniform> spawn_info: vec4f;

fn hash(value: u32) -> u32 {
    var state = value * 747796405u + 2891336453u;
    let word = ((state >> ((state >> 28u) + 4u)) ^ state) * 277803737u;
    return (word >> 22u) ^ word;
}

fn random(seed: ptr<function, u32>) -> f32 {
    *seed = hash(*seed);
    return f32(*seed) / 4294967295.0;
}

fn random_range(seed: ptr<function, u32>, range: vec2f) -> f32 {
    return mix(range.x, range.y, random(seed));
}

fn random_direction(seed: ptr<function, u32>) -> vec3f {
    let z = random(seed) * 2.0 - 1.0;
    let angle = random(seed) * 6.2831853;
    let r = sqrt(max(0.0, 1.0 - z * z));
    return vec3f(r * cos(angle), r * sin(angle), z);
}

fn cone_direction(seed: ptr<function, u32>, axis: vec3f, cos_spread: f32) -> vec3f {
    let z = mix(1.0, cos_spread, random(seed));
    let angle = random(seed) * 6.2831853;
    let r = sqrt(max(0.0, 1.0 - z * z));
    let up = select(vec3f(0.0, 1.0, 0.0), vec3f(1.0, 0.0, 0.0), abs(axis.y) > 0.99);
    let tangent = normalize(cross(up, axis));
    let bitangent = cross(axis, tangent);
    return tangent * (r * cos(angle)) + bitangent * (r * sin(angle)) + axis * z;
}

fn is_flat() -> bool {
    return params.plane.x > 0.5;
}

fn flat_disc(seed: ptr<function, u32>, radius: f32, edge: bool) -> vec3f {
    let angle = random(seed) * 6.2831853;
    let r = select(radius * sqrt(random(seed)), radius, edge);
    return vec3f(cos(angle) * r, sin(angle) * r, 0.0);
}

fn flat_direction(seed: ptr<function, u32>, axis: vec3f, cos_spread: f32) -> vec3f {
    let spread = acos(clamp(cos_spread, -1.0, 1.0));
    let angle = atan2(axis.y, axis.x) + (random(seed) * 2.0 - 1.0) * spread;
    return vec3f(cos(angle), sin(angle), 0.0);
}

fn spawn_point(seed: ptr<function, u32>) -> vec3f {
    let shape = params.shape;
    let flag = params.extra.z;
    switch (u32(shape.x)) {
        case 1u: {
            if (is_flat()) {
                return flat_disc(seed, shape.y, flag > 0.5);
            }
            let radius = select(shape.y * pow(random(seed), 1.0 / 3.0), shape.y, flag > 0.5);
            return random_direction(seed) * radius;
        }
        case 2u: {
            let point = (vec3f(random(seed), random(seed), random(seed)) * 2.0 - 1.0) * shape.yzw;
            return select(point, vec3f(point.xy, 0.0), is_flat());
        }
        case 3u: {
            if (is_flat()) {
                return flat_disc(seed, shape.y, flag > 0.5);
            }
            let angle = random(seed) * 6.2831853;
            let radius = select(shape.y * sqrt(random(seed)), shape.y, flag > 0.5);
            return vec3f(cos(angle) * radius, 0.0, sin(angle) * radius);
        }
        default: {
            return vec3f(0.0);
        }
    }
}

fn launch_velocity(seed: ptr<function, u32>, local_point: vec3f) -> vec3f {
    let axis = normalize(params.direction.xyz);
    var direction = cone_direction(seed, axis, params.direction.w);
    if (is_flat()) {
        direction = flat_direction(seed, axis, params.direction.w);
    }
    var velocity = direction * random_range(seed, params.life_speed.zw);
    let radial = random_range(seed, params.size_radial.zw);
    if (radial != 0.0 && length(local_point) > 0.0001) {
        velocity += normalize(local_point) * radial;
    }
    return velocity;
}

fn make_particle(seed: ptr<function, u32>, position: vec3f, velocity: vec3f, age: f32) -> Particle {
    var p: Particle;
    p.position = vec4f(position + velocity * age, age);
    p.velocity = vec4f(velocity, random_range(seed, params.life_speed.xy));
    p.misc = vec4f(
        random_range(seed, params.size_radial.xy),
        random_range(seed, params.rotation.xy),
        random_range(seed, params.rotation.zw),
        random(seed)
    );
    return p;
}

fn world_position(position: vec3f) -> vec3f {
    if (params.flags.x > 0.5) {
        return (params.transform * vec4f(position, 1.0)).xyz;
    }
    return position;
}

fn world_velocity(velocity: vec3f) -> vec3f {
    if (params.flags.x > 0.5) {
        return (params.transform * vec4f(velocity, 0.0)).xyz;
    }
    return velocity;
}

fn emit_event(position: vec3f, velocity: vec3f) {
    let max_events = u32(params.plane.w);
    if (max_events == 0u) {
        return;
    }
    let index = atomicAdd(&event_count, 1u);
    if (index < max_events) {
        events[index] = Event(vec4f(world_position(position), 1.0), vec4f(world_velocity(velocity), 0.0));
    }
}

@compute @workgroup_size(64)
fn simulate(@builtin(global_invocation_id) id: vec3u) {
    let capacity = u32(params.spawn.z);
    let index = id.x;
    if (index >= capacity) {
        return;
    }
    var p = particles[index];
    if (params.extra.x > 0.5) {
        p.position.w = 0.0;
        p.velocity.w = 0.0;
    }
    let dt = params.previous.w;
    let start = u32(params.spawn.x);
    let count = u32(params.spawn.y);
    let trigger = u32(params.plane.z);
    let relative = (index + capacity - start) % capacity;
    if (relative < count) {
        var seed = hash(index ^ hash(u32(params.spawn.w) + relative * 1013u));
        let t = (f32(relative) + random(&seed)) / f32(count);
        let local_space = params.flags.x > 0.5;
        let local_point = spawn_point(&seed);
        var velocity = launch_velocity(&seed, local_point);
        var position = local_point;
        if (!local_space) {
            position = (params.transform * vec4f(local_point, 1.0)).xyz;
            velocity = (params.transform * vec4f(velocity, 0.0)).xyz;
            let current = params.transform[3].xyz;
            position += mix(params.previous.xyz, current, t) - current;
            if (dt > 0.0) {
                velocity += (current - params.previous.xyz) / dt * params.extra.w;
            }
        }
        if (is_flat()) {
            velocity.z = 0.0;
            position.z = select(params.transform[3].z, 0.0, local_space);
        }
        p = make_particle(&seed, position, velocity, (1.0 - t) * dt);
        if (trigger == 2u) {
            emit_event(p.position.xyz, p.velocity.xyz);
        }
    } else if (p.position.w < p.velocity.w) {
        var velocity = p.velocity.xyz + params.gravity.xyz * dt;
        velocity = velocity / (1.0 + params.gravity.w * dt);
        if (is_flat()) {
            velocity.z = 0.0;
        }
        p.position = vec4f(p.position.xyz + velocity * dt, p.position.w + dt);
        p.velocity = vec4f(velocity, p.velocity.w);
        p.misc.y += p.misc.z * dt;
        if (trigger == 1u && p.position.w >= p.velocity.w) {
            emit_event(p.position.xyz, p.velocity.xyz);
        }
    }
    particles[index] = p;
}

@compute @workgroup_size(64)
fn spawn_events(@builtin(global_invocation_id) id: vec3u) {
    let capacity = u32(params.spawn.z);
    let per_event = u32(spawn_info.x);
    if (capacity == 0u || per_event == 0u) {
        return;
    }
    let event_index = id.x / per_event;
    let total = min(atomicLoad(&source_count), u32(spawn_info.y));
    if (event_index >= total) {
        return;
    }
    let slot = atomicAdd(&spawn_cursor, 1u) % capacity;
    let event = source_events[event_index];
    var seed = hash(id.x ^ hash(u32(params.spawn.w) * 7919u + 17u));
    let local_point = spawn_point(&seed);
    var velocity = launch_velocity(&seed, local_point) + event.velocity.xyz * params.extra.w;
    var position = event.position.xyz + local_point;
    if (is_flat()) {
        velocity.z = 0.0;
        position.z = event.position.z;
    }
    particles[slot] = make_particle(&seed, position, velocity, 0.0);
}
"""

        const val SORT_WGSL =
            PARAMS_WGSL +
                """
struct SortInfo {
    camera_position: vec4f,
    camera_forward: vec4f,
    counts: vec4f,
};

@group(0) @binding(0) var<uniform> params: Params;
@group(0) @binding(1) var<storage, read_write> particles: array<Particle>;
@group(1) @binding(0) var<storage, read_write> keys: array<f32>;
@group(1) @binding(1) var<storage, read_write> order: array<u32>;
@group(1) @binding(2) var<uniform> info: SortInfo;
@group(2) @binding(0) var<uniform> step: vec4f;

@compute @workgroup_size(64)
fn sort_keys(@builtin(global_invocation_id) id: vec3u) {
    let i = id.x;
    let size = u32(info.counts.y);
    if (i >= size) {
        return;
    }
    order[i] = i;
    if (i >= u32(info.counts.x)) {
        keys[i] = -3.0e38;
        return;
    }
    let p = particles[i];
    if (p.position.w >= p.velocity.w) {
        keys[i] = -3.0e38;
        return;
    }
    var position = p.position.xyz;
    if (params.flags.x > 0.5) {
        position = (params.transform * vec4f(position, 1.0)).xyz;
    }
    keys[i] = dot(position - info.camera_position.xyz, info.camera_forward.xyz);
}

@compute @workgroup_size(64)
fn sort_step(@builtin(global_invocation_id) id: vec3u) {
    let i = id.x;
    let size = u32(info.counts.y);
    if (i >= size) {
        return;
    }
    let k = u32(step.x);
    let j = u32(step.y);
    let l = i ^ j;
    if (l <= i) {
        return;
    }
    let a = keys[i];
    let b = keys[l];
    let descending = (i & k) == 0u;
    if ((descending && a < b) || (!descending && a > b)) {
        keys[i] = b;
        keys[l] = a;
        let swap = order[i];
        order[i] = order[l];
        order[l] = swap;
    }
}
"""

        const val VIEW_WGSL =
            """
struct Camera {
    view_proj: mat4x4f,
    right: vec4f,
    up: vec4f,
    back: vec4f,
    inv_proj: mat4x4f,
    viewport: vec4f,
};

@group(0) @binding(0) var<uniform> camera: Camera;
@group(1) @binding(0) var<uniform> params: Params;
@group(1) @binding(1) var<storage, read> particles: array<Particle>;
@group(1) @binding(2) var<storage, read> order: array<u32>;
@group(2) @binding(0) var particle_texture: texture_2d<f32>;
@group(2) @binding(1) var particle_sampler: sampler;

fn key_index(t: f32, count: f32) -> vec2f {
    let scaled = clamp(t, 0.0, 1.0) * max(count - 1.0, 0.0);
    let index = min(floor(scaled), max(count - 2.0, 0.0));
    return vec2f(index, scaled - index);
}

fn size_at(t: f32) -> f32 {
    let count = params.flags.w;
    if (count < 1.5) {
        return params.sizes.x;
    }
    let key = key_index(t, count);
    let a = params.sizes[u32(key.x)];
    let b = params.sizes[u32(key.x) + 1u];
    return mix(a, b, clamp(key.y, 0.0, 1.0));
}

fn color_at(t: f32) -> vec4f {
    let count = params.flags.z;
    if (count < 1.5) {
        return params.colors[0];
    }
    let key = key_index(t, count);
    return mix(params.colors[u32(key.x)], params.colors[u32(key.x) + 1u], clamp(key.y, 0.0, 1.0));
}

fn is_dead(p: Particle) -> bool {
    return p.position.w >= p.velocity.w;
}
"""

        const val RENDER_WGSL =
            PARAMS_WGSL +
                VIEW_WGSL +
                    """
@group(3) @binding(0) var scene_depth: texture_depth_2d;

struct VertexOutput {
    @builtin(position) position: vec4f,
    @location(0) color: vec4f,
    @location(1) uv: vec2f,
    @location(2) uv2: vec2f,
    @location(3) frame_blend: f32,
    @location(4) rotation: vec2f,
};

fn frame_uv(local: vec2f, frame: f32) -> vec2f {
    let columns = params.sheet.x;
    let row = floor(frame / columns);
    let column = frame - row * columns;
    return vec2f((column + local.x) / columns, (row + local.y) / params.sheet.y);
}

@vertex
fn vs_main(@builtin(vertex_index) vertex: u32, @builtin(instance_index) instance: u32) -> VertexOutput {
    var output: VertexOutput;
    let p = particles[order[instance]];
    if (is_dead(p)) {
        output.position = vec4f(0.0, 0.0, 2.0, 1.0);
        output.color = vec4f(0.0);
        output.uv = vec2f(0.0);
        output.uv2 = vec2f(0.0);
        output.frame_blend = 0.0;
        output.rotation = vec2f(1.0, 0.0);
        return output;
    }
    var corners = array<vec2f, 6>(
        vec2f(-0.5, -0.5), vec2f(0.5, -0.5), vec2f(0.5, 0.5),
        vec2f(-0.5, -0.5), vec2f(0.5, 0.5), vec2f(-0.5, 0.5)
    );
    let corner = corners[vertex];
    let t = p.position.w / p.velocity.w;
    let size = p.misc.x * size_at(t);
    let c = cos(p.misc.y);
    let s = sin(p.misc.y);
    let rotated = vec2f(corner.x * c - corner.y * s, corner.x * s + corner.y * c);
    var center = p.position.xyz;
    var velocity = p.velocity.xyz;
    if (params.flags.x > 0.5) {
        center = (params.transform * vec4f(center, 1.0)).xyz;
        velocity = (params.transform * vec4f(velocity, 0.0)).xyz;
    }
    var offset: vec3f;
    switch (u32(params.flags.y)) {
        case 1u: {
            let speed = length(velocity);
            let axis = select(camera.up.xyz, velocity / max(speed, 0.0001), speed > 0.0001);
            var side = cross(axis, camera.back.xyz);
            let side_length = length(side);
            side = select(camera.right.xyz, side / max(side_length, 0.0001), side_length > 0.0001);
            offset = side * (corner.x * size) + axis * (corner.y * size * (1.0 + speed * params.extra.y));
        }
        case 2u: {
            offset = vec3f(rotated.x, 0.0, rotated.y) * size;
        }
        default: {
            offset = camera.right.xyz * (rotated.x * size) + camera.up.xyz * (rotated.y * size);
        }
    }
    output.position = camera.view_proj * vec4f(center + offset, 1.0);
    output.color = color_at(t);
    output.rotation = vec2f(c, s);
    let local_uv = vec2f(corner.x + 0.5, 0.5 - corner.y);
    output.uv = local_uv;
    output.uv2 = local_uv;
    output.frame_blend = 0.0;
    let frames = params.sheet.z;
    if (frames > 1.5) {
        var frame = select(t * frames, p.position.w * params.sheet.w, params.sheet.w > 0.0);
        if (params.sheet_flags.y > 0.5) {
            frame += floor(p.misc.w * frames);
        }
        var first: f32;
        var second: f32;
        if (params.sheet_flags.x > 0.5) {
            frame = frame - floor(frame / frames) * frames;
            first = floor(frame);
            second = select(first + 1.0, 0.0, first + 1.0 >= frames);
        } else {
            frame = min(frame, frames - 1.0);
            first = floor(frame);
            second = min(first + 1.0, frames - 1.0);
        }
        output.uv = frame_uv(local_uv, first);
        output.uv2 = frame_uv(local_uv, second);
        output.frame_blend = select(0.0, frame - first, params.sheet_flags.z > 0.5);
    }
    return output;
}

fn sample_particle(input: VertexOutput) -> vec4f {
    let a = textureSample(particle_texture, particle_sampler, input.uv);
    let b = textureSample(particle_texture, particle_sampler, input.uv2);
    return mix(a, b, input.frame_blend) * input.color;
}

fn view_depth(position: vec2f, depth: f32) -> f32 {
    let ndc = vec4f(
        position.x / camera.viewport.x * 2.0 - 1.0,
        1.0 - position.y / camera.viewport.y * 2.0,
        depth,
        1.0
    );
    let view = camera.inv_proj * ndc;
    return view.z / view.w;
}

fn soft_fade(position: vec4f, softness: f32) -> f32 {
    let scene = textureLoad(scene_depth, vec2i(position.xy), 0);
    let gap = view_depth(position.xy, position.z) - view_depth(position.xy, scene);
    return clamp(gap / softness, 0.0, 1.0);
}

@fragment
fn fs_main(input: VertexOutput) -> @location(0) vec4f {
    let color = sample_particle(input);
    if (color.a <= 0.002) {
        discard;
    }
    return color;
}

@fragment
fn fs_soft(input: VertexOutput) -> @location(0) vec4f {
    var color = sample_particle(input);
    color.a *= soft_fade(input.position, params.plane.y);
    if (color.a <= 0.002) {
        discard;
    }
    return color;
}

@fragment
fn fs_opaque(input: VertexOutput) -> @location(0) vec4f {
    let color = sample_particle(input);
    if (color.a < 0.5) {
        discard;
    }
    return vec4f(color.rgb, 1.0);
}

fn distortion(input: VertexOutput, fade: f32) -> vec4f {
    let a = textureSample(particle_texture, particle_sampler, input.uv);
    let b = textureSample(particle_texture, particle_sampler, input.uv2);
    let map = mix(a, b, input.frame_blend);
    let push = map.rg * 2.0 - 1.0;
    let up = vec2f(push.x, -push.y);
    let turned = vec2f(
        up.x * input.rotation.x - up.y * input.rotation.y,
        up.x * input.rotation.y + up.y * input.rotation.x
    );
    let amount = map.a * input.color.a * fade;
    return vec4f(turned.x * amount, -turned.y * amount, 0.0, amount);
}

@fragment
fn fs_distort(input: VertexOutput) -> @location(0) vec4f {
    return distortion(input, 1.0);
}

@fragment
fn fs_distort_soft(input: VertexOutput) -> @location(0) vec4f {
    return distortion(input, soft_fade(input.position, max(params.plane.y, 0.05)));
}
"""

        const val RENDER_MESH_WGSL =
            PARAMS_WGSL +
                VIEW_WGSL +
                    """
struct MeshVertex {
    position: vec4f,
    normal: vec4f,
    uv: vec4f,
};

struct Light {
    direction: vec4f,
    color: vec4f,
    ambient: vec4f,
};

@group(3) @binding(0) var<storage, read> vertices: array<MeshVertex>;
@group(3) @binding(1) var<uniform> light: Light;

struct MeshOutput {
    @builtin(position) position: vec4f,
    @location(0) color: vec4f,
    @location(1) uv: vec2f,
    @location(2) normal: vec3f,
};

fn turn(v: vec3f, axis: vec3f, angle: f32) -> vec3f {
    let c = cos(angle);
    let s = sin(angle);
    return v * c + cross(axis, v) * s + axis * dot(axis, v) * (1.0 - c);
}

@vertex
fn vs_mesh(@builtin(vertex_index) vertex: u32, @builtin(instance_index) instance: u32) -> MeshOutput {
    var output: MeshOutput;
    let p = particles[order[instance]];
    if (is_dead(p)) {
        output.position = vec4f(0.0, 0.0, 2.0, 1.0);
        output.color = vec4f(0.0);
        output.uv = vec2f(0.0);
        output.normal = vec3f(0.0, 1.0, 0.0);
        return output;
    }
    let mesh_vertex = vertices[vertex];
    let t = p.position.w / p.velocity.w;
    let size = p.misc.x * size_at(t);
    var center = p.position.xyz;
    var velocity = p.velocity.xyz;
    if (params.flags.x > 0.5) {
        center = (params.transform * vec4f(center, 1.0)).xyz;
        velocity = (params.transform * vec4f(velocity, 0.0)).xyz;
    }
    var local = mesh_vertex.position.xyz * size;
    var normal = mesh_vertex.normal.xyz;
    let y_axis = vec3f(0.0, 1.0, 0.0);
    switch (u32(params.flags.y)) {
        case 1u: {
            let speed = length(velocity);
            let axis = select(y_axis, velocity / max(speed, 0.0001), speed > 0.0001);
            local.y *= 1.0 + speed * params.extra.y;
            local = turn(local, y_axis, p.misc.y);
            normal = turn(normal, y_axis, p.misc.y);
            let hinge = cross(y_axis, axis);
            let hinge_length = length(hinge);
            if (hinge_length > 0.0001) {
                let angle = atan2(hinge_length, axis.y);
                local = turn(local, hinge / hinge_length, angle);
                normal = turn(normal, hinge / hinge_length, angle);
            } else if (axis.y < 0.0) {
                local = vec3f(local.x, -local.y, -local.z);
                normal = vec3f(normal.x, -normal.y, -normal.z);
            }
        }
        case 2u: {
            local = turn(local, y_axis, p.misc.y);
            normal = turn(normal, y_axis, p.misc.y);
        }
        default: {
            let z = fract(p.misc.w * 7.31) * 2.0 - 1.0;
            let around = p.misc.w * 6.2831853;
            let r = sqrt(max(0.0, 1.0 - z * z));
            let axis = vec3f(r * cos(around), z, r * sin(around));
            local = turn(local, axis, p.misc.y);
            normal = turn(normal, axis, p.misc.y);
        }
    }
    if (params.flags.x > 0.5) {
        local = (params.transform * vec4f(local, 0.0)).xyz;
        normal = (params.transform * vec4f(normal, 0.0)).xyz;
    }
    output.position = camera.view_proj * vec4f(center + local, 1.0);
    output.color = color_at(t);
    output.uv = mesh_vertex.uv.xy;
    output.normal = normal;
    return output;
}

fn shade(input: MeshOutput) -> vec4f {
    let albedo = textureSample(particle_texture, particle_sampler, input.uv) * input.color;
    let n = normalize(input.normal);
    let l = -light.direction.xyz;
    let v = camera.back.xyz;
    let diffuse = max(dot(n, l), 0.0);
    let h = normalize(l + v);
    let specular = pow(max(dot(n, h), 0.0), max(params.mesh.y, 1.0)) * step(0.0001, diffuse);
    let rim = pow(1.0 - max(dot(n, v), 0.0), 3.0) * 0.25;
    let lit = albedo.rgb * (light.ambient.rgb + light.color.rgb * diffuse) + light.color.rgb * specular * 0.5 + albedo.rgb * rim;
    return vec4f(mix(lit, albedo.rgb, clamp(params.mesh.x, 0.0, 1.0)), albedo.a);
}

@fragment
fn fs_mesh(input: MeshOutput) -> @location(0) vec4f {
    let color = shade(input);
    if (color.a <= 0.002) {
        discard;
    }
    return color;
}

@fragment
fn fs_mesh_opaque(input: MeshOutput) -> @location(0) vec4f {
    let color = shade(input);
    if (color.a < 0.5) {
        discard;
    }
    return vec4f(color.rgb, 1.0);
}
"""
    }
}
