package com.littlekt.graphics.g2d

import com.littlekt.EngineStats
import com.littlekt.Graphics
import com.littlekt.graphics.Color
import com.littlekt.graphics.IndexedMesh
import com.littlekt.graphics.Texture
import com.littlekt.graphics.TextureInvalidationListener
import com.littlekt.graphics.g2d.util.CameraSpriteBuffers
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.textureIndexedMesh
import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.util.CameraBuffersViaMatrix
import com.littlekt.graphics.util.CommonIndexedMeshGeometry
import com.littlekt.graphics.util.TextureBindGroupCache
import com.littlekt.graphics.webgpu.*
import com.littlekt.log.Logger
import com.littlekt.math.Mat4
import com.littlekt.math.geom.Angle
import com.littlekt.math.geom.cosine
import com.littlekt.math.geom.normalized
import com.littlekt.math.geom.sine
import com.littlekt.math.isFuzzyZero
import com.littlekt.util.LazyMat4
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * Draws batched quads using indices.
 *
 * @param device the device.
 * @param initWidth the initial width to use when calculating the projection matrix.
 * @param initHeight the initial height to sue when calculating the projection matrix.
 * @param format the texture format to be used for each [RenderPipeline].
 * @param size the initial number of quads to be used by the buffers in the internal mesh. The mesh
 *   will automatically grow if needed.
 * @author Colton Daily
 * @date 4/11/2024
 */
class SpriteBatch(
    val device: Device,
    initWidth: Int,
    initHeight: Int,
    val format: TextureFormat,
    private val size: Int = 1000,
    cameraBuffers: CameraBuffersViaMatrix? = null,
) : Batch {

    constructor(
        device: Device,
        graphics: Graphics,
        format: TextureFormat,
        size: Int = 1000,
        cameraBuffers: CameraBuffersViaMatrix? = null,
    ) : this(device, graphics.width, graphics.height, format, size, cameraBuffers)

    private val ownCameraBuffers = cameraBuffers == null
    private val cameraBuffers: CameraBuffersViaMatrix = cameraBuffers ?: CameraSpriteBuffers(device)

    /**
     * The transform matrix that can be used to multiply against the [viewProjection] matrix. This
     * should be set directly instead of manipulating the underlying matrix.
     */
    override var transformMatrix = Mat4()
        set(value) {
            field = value
            combinedMatrix.isDirty = true
        }

    /**
     * The view projection matrix to be used when rendering. This should be set directly instead of
     * manipulating the underlying matrix.
     */
    override var viewProjection =
        Mat4()
            .setToOrthographic(
                left = -initWidth * 0.5f,
                right = initWidth * 0.5f,
                bottom = -initHeight * 0.5f,
                top = initHeight * 0.5f,
                near = 0f,
                far = 1f,
            )
        set(value) {
            field = value
            combinedMatrix.isDirty = true
        }

    override var drawing: Boolean = false

    override var lastMeshIdx: Int = 0

    private var combinedMatrix = LazyMat4 { it.set(viewProjection).mul(transformMatrix) }

    private val meshes: MutableList<IndexedMesh<CommonIndexedMeshGeometry>> =
        mutableListOf(textureIndexedMesh(device, size) { indicesAsQuad() })
    private val mesh: IndexedMesh<CommonIndexedMeshGeometry>
        get() = meshes[lastMeshIdx]

    override val defaultShader: Shader = SpriteBatchShader(device)

    override var shader: Shader = defaultShader

    private val bindGroups =
        TextureBindGroupCache<BindGroup>(DEFAULT_BIND_GROUP_CACHE_SIZE) { it.release() }
    private val evictionLock = SynchronizedObject()
    private var pendingEvictions = IntArray(16)
    private var pendingEvictionCount = 0
    private val hasPendingEvictions = atomic(false)
    private val textureListener = TextureInvalidationListener { id ->
        synchronized(evictionLock) {
            if (pendingEvictionCount == pendingEvictions.size) {
                pendingEvictions = pendingEvictions.copyOf(pendingEvictions.size * 2)
            }
            pendingEvictions[pendingEvictionCount++] = id
            hasPendingEvictions.value = true
        }
    }
    private val drawCalls = SpriteDrawCalls()
    private val renderInfos = ArrayList<RenderInfo>()

    /**
     * The max number of texture bind groups cached across shaders. The least recently used ones
     * are released once there are more. Bind groups of released or recreated textures are always
     * released.
     */
    var bindGroupCacheSize: Int
        get() = bindGroups.maxSize
        set(value) {
            bindGroups.maxSize = value
        }

    private val scissor = ScissorRect()
    private val lastScissor = ScissorRect()

    private var blendState = BlendState.NonPreMultiplied
    private var prevBlendState = blendState

    /**
     * The format of the depth target of the render pass, or `null` if it has none. Set it to draw
     * within a 3D pass, such as panels, signs & text placed in the world with [transformMatrix].
     */
    var depthFormat: TextureFormat? = null

    /** If `true` and [depthFormat] is set, the following draws are hidden behind closer geometry. */
    var depthTest: Boolean = true

    /** If `true` and [depthFormat] is set, the following draws write their depth. */
    var depthWrite: Boolean = false

    /** The number of samples of the render pass targets, `1` or `4`. */
    var sampleCount: Int = 1
        set(value) {
            require(value == 1 || value == 4) { "SpriteBatch.sampleCount must be 1 or 4!" }
            field = value
        }

    private val renderPipelineByBlendState: MutableMap<RenderInfo, RenderPipeline> =
        mutableMapOf(
            RenderInfo(shader, blendState) to
                device.createRenderPipeline(
                    createRenderPipelineDescriptor(RenderInfo(shader, blendState))
                )
        )

    private var meshSprites = IntArray(4)
    private var spriteIdx: Int
        get() = if (lastMeshIdx < meshSprites.size) meshSprites[lastMeshIdx] else 0
        set(value) {
            if (lastMeshIdx >= meshSprites.size) meshSprites = meshSprites.copyOf(lastMeshIdx * 2 + 1)
            meshSprites[lastMeshIdx] = value
        }

    private val currentDrawCall: SpriteDrawCall
        get() = drawCalls.current ?: error("SpriteBatch has no draw call!")

    private var lastTexture: Texture? = null
    private var lastBlendState: BlendState = blendState
    private var lastDepthFormat: TextureFormat? = null
    private var lastDepthTest = true
    private var lastDepthWrite = false
    private var lastSampleCount = 1
    private var lastShader: Shader = shader
    private val lastCombinedMatrix: Mat4 = Mat4().set(combinedMatrix.get())
    private var invTexWidth = 0f
    private var invTexHeight = 0f

    private var lastDynamicOffsetIndex = -1L

    init {
        check(size > 0) { "A batch must be greater than zero sprites!" }
        Texture.addInvalidationListener(textureListener)
    }

    /**
     * Clips the following draws to a scissor rect, in pixels of the render target from its top
     * left. Changing it doesn't flush: each flush draws every range with its own scissor rect in
     * the same render pass, without using more camera dynamic offsets.
     *
     * The rect must be within the render target. Negative values are clamped to `0`.
     */
    fun setScissorRect(x: Int, y: Int, width: Int, height: Int) {
        scissor.set(
            if (x < 0) 0 else x,
            if (y < 0) 0 else y,
            if (width < 0) 0 else width,
            if (height < 0) 0 else height,
        )
    }

    /**
     * Stops clipping the following draws with [setScissorRect]. They keep the render pass's
     * current scissor rect, so once the batch has set one in a render pass, use [setScissorRect]
     * with the full render target instead to draw unclipped in that pass.
     */
    fun clearScissorRect() {
        scissor.clear()
    }

    override fun begin(viewProjection: Mat4?) {
        check(!drawing) { "SpriteBatch.end must be called before begin." }
        viewProjection?.let { this.viewProjection = it }
        drawing = true
    }

    override fun draw(
        slice: TextureSlice,
        x: Float,
        y: Float,
        originX: Float,
        originY: Float,
        width: Float,
        height: Float,
        scaleX: Float,
        scaleY: Float,
        rotation: Angle,
        color: Color,
        flipX: Boolean,
        flipY: Boolean,
    ) {
        check(drawing) { "SpriteBatch.begin must be called before draw." }
        ensureDrawCall(slice.texture)

        var fx = -(originX - slice.offsetX)
        var fy = -(originY - slice.offsetY)
        val w = if (slice.rotated) height else width
        val h = if (slice.rotated) width else height
        var fx2 = w + fx
        var fy2 = h + fy

        if (scaleX != 1f || scaleY != 1f) {
            fx *= scaleX
            fy *= scaleY
            fx2 *= scaleX
            fy2 *= scaleY
        }

        val p1x = fx
        val p1y = fy
        val p2x = fx
        val p2y = fy2
        val p3x = fx2
        val p3y = fy2
        val p4x = fx2
        val p4y = fy

        var blX: Float
        var blY: Float
        var tlX: Float
        var tlY: Float
        var trX: Float
        var trY: Float
        var brX: Float
        var brY: Float

        if (rotation.normalized.radians.isFuzzyZero()) {
            blX = p1x
            blY = p1y

            tlX = p2x
            tlY = p2y

            trX = p3x
            trY = p3y

            brX = p4x
            brY = p4y
        } else {
            val cos = rotation.cosine
            val sin = rotation.sine

            blX = cos * p1x - sin * p1y
            blY = sin * p1x + cos * p1y

            tlX = cos * p2x - sin * p2y
            tlY = sin * p2x + cos * p2y

            trX = cos * p3x - sin * p3y
            trY = sin * p3x + cos * p3y

            brX = blX + (trX - tlX)
            brY = trY - (tlY - blY)
        }

        blX += x
        blY += y
        tlX += x
        tlY += y
        trX += x
        trY += y
        brX += x
        brY += y

        val u0 = if (flipX) slice.u1 else slice.u
        val v0 = if (flipY) slice.v1 else slice.v
        val u1 = if (flipX) slice.u else slice.u1
        val v1 = if (flipY) slice.v else slice.v1

        mesh.geometry.run {
            addVertex { // top left
                position.x = tlX
                position.y = tlY
                this.color.set(color)
                uv.x = if (slice.rotated) u1 else u0
                uv.y = v0
            }
            addVertex { // top right
                position.x = trX
                position.y = trY
                this.color.set(color)
                uv.x = u1
                uv.y = if (slice.rotated) v1 else v0
            }
            addVertex { // bottom right
                position.x = brX
                position.y = brY
                this.color.set(color)
                uv.x = if (slice.rotated) u0 else u1
                uv.y = v1
            }
            addVertex { // bottom left
                position.x = blX
                position.y = blY
                this.color.set(color)
                uv.x = u0
                uv.y = if (slice.rotated) v0 else v1
            }
        }

        currentDrawCall.instances++
        spriteIdx++
    }

    override fun draw(
        slice: TextureSlice,
        x: Float,
        y: Float,
        originX: Float,
        originY: Float,
        width: Float,
        height: Float,
        scaleX: Float,
        scaleY: Float,
        rotation: Angle,
        color: Color,
        srcX: Int,
        srcY: Int,
        srcWidth: Int,
        srcHeight: Int,
        flipX: Boolean,
        flipY: Boolean,
    ) {
        check(drawing) { "SpriteBatch.begin must be called before draw." }
        ensureDrawCall(slice.texture)

        var fx = -(originX - slice.offsetX)
        var fy = -(originY - slice.offsetY)
        val w = if (slice.rotated) height else width
        val h = if (slice.rotated) width else height
        var fx2 = w + fx
        var fy2 = h + fy

        if (scaleX != 1f || scaleY != 1f) {
            fx *= scaleX
            fy *= scaleY
            fx2 *= scaleX
            fy2 *= scaleY
        }

        val p1x = fx
        val p1y = fy
        val p2x = fx
        val p2y = fy2
        val p3x = fx2
        val p3y = fy2
        val p4x = fx2
        val p4y = fy

        var blX: Float
        var blY: Float
        var tlX: Float
        var tlY: Float
        var trX: Float
        var trY: Float
        var brR: Float
        var byR: Float

        if (rotation.normalized.radians.isFuzzyZero()) {
            blX = p1x
            blY = p1y

            tlX = p2x
            tlY = p2y

            trX = p3x
            trY = p3y

            brR = p4x
            byR = p4y
        } else {
            val cos = rotation.cosine
            val sin = rotation.sine

            blX = cos * p1x - sin * p1y
            blY = sin * p1x + cos * p1y

            tlX = cos * p2x - sin * p2y
            tlY = sin * p2x + cos * p2y

            trX = cos * p3x - sin * p3y
            trY = sin * p3x + cos * p3y

            brR = blX + (trX - tlX)
            byR = trY - (tlY - blY)
        }

        blX += x
        blY += y
        tlX += x
        tlY += y
        trX += x
        trY += y
        brR += x
        byR += y

        var u0 = srcX * invTexWidth
        var v0 = srcY * invTexHeight
        var u1 = (srcX + srcWidth) * invTexWidth
        var v1 = (srcY + srcHeight) * invTexHeight

        u0 = if (flipX) u1 else u0
        v0 = if (flipY) v1 else v0
        u1 = if (flipX) u0 else u1
        v1 = if (flipY) v0 else v1

        mesh.geometry.run {
            addVertex { // top left
                position.x = tlX
                position.y = tlY
                this.color.set(color)
                uv.x = if (slice.rotated) u1 else u0
                uv.y = v0
            }
            addVertex { // top right
                position.x = trX
                position.y = trY
                this.color.set(color)
                uv.x = u1
                uv.y = if (slice.rotated) v1 else v0
            }
            addVertex { // bottom right
                position.x = brR
                position.y = byR
                this.color.set(color)
                uv.x = if (slice.rotated) u0 else u1
                uv.y = v1
            }
            addVertex { // bottom left
                position.x = blX
                position.y = blY
                this.color.set(color)
                uv.x = u0
                uv.y = if (slice.rotated) v0 else v1
            }
        }

        currentDrawCall.instances++
        spriteIdx++
    }

    override fun draw(
        texture: Texture,
        x: Float,
        y: Float,
        originX: Float,
        originY: Float,
        width: Float,
        height: Float,
        scaleX: Float,
        scaleY: Float,
        rotation: Angle,
        srcX: Int,
        srcY: Int,
        srcWidth: Int,
        srcHeight: Int,
        color: Color,
        flipX: Boolean,
        flipY: Boolean,
    ) {
        check(drawing) { "SpriteBatch.begin must be called before draw." }
        ensureDrawCall(texture)

        var fx = -originX
        var fy = -originY
        var fx2 = width - originX
        var fy2 = height - originY

        if (scaleX != 1f || scaleY != 1f) {
            fx *= scaleX
            fy *= scaleY
            fx2 *= scaleX
            fy2 *= scaleY
        }

        val p1x = fx
        val p1y = fy
        val p2x = fx
        val p2y = fy2
        val p3x = fx2
        val p3y = fy2
        val p4x = fx2
        val p4y = fy

        var blX: Float
        var blY: Float
        var tlX: Float
        var tlY: Float
        var trX: Float
        var trY: Float
        var brX: Float
        var brY: Float

        if (rotation.normalized.radians.isFuzzyZero()) {
            blX = p1x
            blY = p1y

            tlX = p2x
            tlY = p2y

            trX = p3x
            trY = p3y

            brX = p4x
            brY = p4y
        } else {
            val cos = rotation.cosine
            val sin = rotation.sine

            blX = cos * p1x - sin * p1y
            blY = sin * p1x + cos * p1y

            tlX = cos * p2x - sin * p2y
            tlY = sin * p2x + cos * p2y

            trX = cos * p3x - sin * p3y
            trY = sin * p3x + cos * p3y

            brX = blX + (trX - tlX)
            brY = trY - (tlY - blY)
        }

        blX += x
        blY += y
        tlX += x
        tlY += y
        trX += x
        trY += y
        brX += x
        brY += y

        var u0 = srcX * invTexWidth
        var v0 = srcY * invTexHeight
        var u1 = (srcX + srcWidth) * invTexWidth
        var v1 = (srcY + srcHeight) * invTexHeight

        if (flipX) {
            val tmp = u0
            u0 = u1
            u1 = tmp
        }

        if (flipY) {
            val tmp = v0
            v0 = v1
            v1 = tmp
        }

        mesh.geometry.run {
            addVertex { // top left
                position.x = tlX
                position.y = tlY
                this.color.set(color)
                uv.x = u0
                uv.y = v0
            }
            addVertex { // top right
                position.x = trX
                position.y = trY
                this.color.set(color)
                uv.x = u1
                uv.y = v0
            }
            addVertex { // bottom right
                position.x = brX
                position.y = brY
                this.color.set(color)
                uv.x = u1
                uv.y = v1
            }
            addVertex { // bottom left
                position.x = blX
                position.y = blY
                this.color.set(color)
                uv.x = u0
                uv.y = v1
            }
        }

        currentDrawCall.instances++
        spriteIdx++
    }

    override fun draw(texture: Texture, spriteVertices: FloatArray, offset: Int, count: Int) {
        check(drawing) { "SpriteBatch.begin must be called before draw." }
        ensureDrawCall(texture)

        val stride = mesh.geometry.vertexStride

        mesh.geometry.add(spriteVertices, offset, spriteIdx * stride, count)
        val total = count / stride
        currentDrawCall.instances += total
        spriteIdx += total
    }

    override fun flush(renderPassEncoder: RenderPassEncoder, viewProjection: Mat4?) {
        check(drawing) { "SpriteBatch.begin must be called before draw." }
        viewProjection?.let { this.viewProjection = it }
        if (spriteIdx == 0) return

        evictInvalidatedTextures()
        mesh.update()
        renderPassEncoder.setIndexBuffer(mesh.ibo, IndexFormat.UINT16)
        renderPassEncoder.setVertexBuffer(0, mesh.vbo)
        var lastPipelineSet: RenderPipeline? = null
        var lastBindGroupSet: BindGroup? = null
        var lastShader: Shader? = null
        for (i in 0 until drawCalls.size) {
            val drawCall = drawCalls[i]
            val renderInfo = drawCall.renderInfo as RenderInfo
            val shader = renderInfo.shader
            val renderPipeline =
                renderPipelineByBlendState.getOrPut(renderInfo) {
                    device.createRenderPipeline(createRenderPipelineDescriptor(renderInfo))
                }
            val texture = drawCall.texture
            val view = texture.view
            val sampler = texture.sampler
            val textureBindGroup =
                bindGroups[shader.id, texture.id, view, sampler]
                    ?: (shader.createBindGroup(BindingUsage.TEXTURE, view, sampler)
                        ?: error(
                            "SpriteBatch requires ${shader::class.simpleName} to create a bindgroup for BindingUsage.TEXTURE but it failed to do so."
                        ))
                        .also { bindGroups.put(shader.id, texture.id, view, sampler, it) }
            if (drawCall.matrixChanged) {
                if (lastDynamicOffsetIndex < cameraBuffers.cameraDynamicSize - 1) {
                    lastDynamicOffsetIndex++
                } else {
                    logger.warn {
                        "SpriteBatch wants to update the SpriteShader.CAMERA_UNIFORM_DYNAMIC_OFFSET but is unable to due to SpriteBatch.cameraDynamicSize being too small! If you are setting the Batch.viewProjection multiple times per render pass then increase this value!"
                    }
                }
                cameraBuffers.update(
                    drawCall.combinedMatrix,
                    dynamicOffset = lastDynamicOffsetIndex,
                )
            }
            if (lastPipelineSet != renderPipeline) {
                renderPassEncoder.setPipeline(renderPipeline)
                lastPipelineSet = renderPipeline
            }
            if (lastBindGroupSet != textureBindGroup) {
                lastBindGroupSet = textureBindGroup
                shader.setBindGroup(renderPassEncoder, textureBindGroup, BindingUsage.TEXTURE)
            }
            if (lastShader != shader || drawCall.matrixChanged) {
                shader.setBindGroup(
                    renderPassEncoder,
                    cameraBuffers.getOrCreateBindGroup(shader),
                    cameraBuffers.bindingUsage,
                    lastDynamicOffsetIndex.toInt() * device.limits.minUniformBufferOffsetAlignment,
                )
                shader.setBindGroups(renderPassEncoder)
                lastShader = shader
            }
            if (drawCall.scissorChanged) {
                val rect = drawCall.scissor
                renderPassEncoder.setScissorRect(rect.x, rect.y, rect.width, rect.height)
            }
            val indexCount = drawCall.instances * 6
            EngineStats.extra(QUAD_STATS_NAME, drawCall.instances)
            renderPassEncoder.drawIndexed(indexCount, 1, firstIndex = drawCall.offset * 6)
        }
        drawCalls.clear()
        lastTexture = null
        lastMeshIdx++
    }

    override fun end() {
        check(drawing) { "SpriteBatch.begin must be called before draw." }
        lastMeshIdx = 0
        lastDynamicOffsetIndex = -1L
        for (i in meshes.indices) meshes[i].clearVertices()
        meshSprites.fill(0)
        drawing = false
    }

    override fun setBlendState(newBlendState: BlendState) {
        if (blendState == newBlendState) return
        prevBlendState = blendState
        blendState = newBlendState
    }

    override fun swapToPreviousBlendState() {
        if (blendState == prevBlendState) return
        setBlendState(prevBlendState)
    }

    override fun useDefaultShader() {
        if (shader != defaultShader) {
            shader = defaultShader
        }
    }

    private fun ensureDrawCall(texture: Texture) {
        ensureMesh()
        ensureTexture(texture)
        ensureRenderInfo()
        ensureMatrices()
        ensureScissor()
    }

    private fun ensureMesh() {
        if (lastMeshIdx + 1 > meshes.size) {
            addMesh()
        }
    }

    private fun ensureTexture(texture: Texture) {
        if (texture != lastTexture) {
            lastTexture = texture
            invTexWidth = 1f / texture.width
            invTexHeight = 1f / texture.height

            createDrawCall()
        }
    }

    private fun ensureRenderInfo() {
        if (
            blendState != lastBlendState ||
                shader != lastShader ||
                depthFormat != lastDepthFormat ||
                depthTest != lastDepthTest ||
                depthWrite != lastDepthWrite ||
                sampleCount != lastSampleCount
        ) {
            lastBlendState = blendState
            lastShader = shader
            lastDepthFormat = depthFormat
            lastDepthTest = depthTest
            lastDepthWrite = depthWrite
            lastSampleCount = sampleCount

            createDrawCall()
        }
    }

    private fun ensureMatrices() {
        if (combinedMatrix.get() != lastCombinedMatrix) {
            lastCombinedMatrix.set(combinedMatrix.get())
            createDrawCall()
        }
    }

    private fun evictInvalidatedTextures() {
        if (!hasPendingEvictions.value) return
        synchronized(evictionLock) {
            for (i in 0 until pendingEvictionCount) bindGroups.evictTexture(pendingEvictions[i])
            pendingEvictionCount = 0
            hasPendingEvictions.value = false
        }
    }

    private fun ensureScissor() {
        if (!scissor.matches(lastScissor)) {
            lastScissor.set(scissor)
            createDrawCall()
        }
    }

    private fun createDrawCall() {
        val texture = lastTexture ?: error("Attempting to create a draw call with no texture!")
        drawCalls.next(texture, currentRenderInfo(), lastCombinedMatrix, lastScissor, spriteIdx)
    }

    private fun currentRenderInfo(): RenderInfo {
        for (i in renderInfos.indices) {
            val info = renderInfos[i]
            if (
                info.shader === lastShader &&
                    info.blendState == lastBlendState &&
                    info.depthFormat == lastDepthFormat &&
                    info.depthTest == lastDepthTest &&
                    info.depthWrite == lastDepthWrite &&
                    info.sampleCount == lastSampleCount
            ) {
                return info
            }
        }
        return RenderInfo(
                lastShader,
                lastBlendState,
                lastDepthFormat,
                lastDepthTest,
                lastDepthWrite,
                lastSampleCount,
            )
            .also { renderInfos += it }
    }

    private fun createRenderPipelineDescriptor(renderInfo: RenderInfo): RenderPipelineDescriptor {
        val shader = renderInfo.shader
        val blendState = renderInfo.blendState
        val depth = renderInfo.depthFormat
        return RenderPipelineDescriptor(
            layout = shader.getOrCreatePipelineLayout(),
            vertex =
                VertexState(
                    module = shader.shaderModule,
                    entryPoint = shader.vertexEntryPoint,
                    buffer = mesh.geometry.layout.gpuVertexBufferLayout,
                ),
            fragment =
                FragmentState(
                    module = shader.shaderModule,
                    entryPoint = shader.fragmentEntryPoint,
                    target =
                        ColorTargetState(
                            format = format,
                            blendState = blendState,
                            writeMask = ColorWriteMask.ALL,
                        ),
                ),
            primitive = PrimitiveState(topology = PrimitiveTopology.TRIANGLE_LIST),
            depthStencil =
                depth?.let {
                    DepthStencilState(
                        format = it,
                        depthWriteEnabled = renderInfo.depthWrite,
                        depthCompare =
                            if (renderInfo.depthTest) CompareFunction.LESS_EQUAL
                            else CompareFunction.ALWAYS,
                    )
                },
            multisample =
                MultisampleState(
                    count = renderInfo.sampleCount,
                    mask = 0xFFFFFFF,
                    alphaToCoverageEnabled = false,
                ),
            label =
                "SpriteBatch RenderPipeline for shader: ${shader::class.simpleName}(${shader.id})",
        )
    }

    private fun addMesh(): IndexedMesh<CommonIndexedMeshGeometry> {
        val mesh = textureIndexedMesh(device, size) { indicesAsQuad() }
        meshes += mesh
        if (meshes.size > meshSprites.size) meshSprites = meshSprites.copyOf(meshes.size * 2)
        return mesh
    }

    override fun release() {
        lastTexture = null
        meshes.forEach { it.release() }
        defaultShader.release()
        renderPipelineByBlendState.values.forEach { it.release() }
        renderPipelineByBlendState.clear()
        Texture.removeInvalidationListener(textureListener)
        bindGroups.clear()
        drawCalls.release()
        renderInfos.clear()
        if (ownCameraBuffers) {
            cameraBuffers.release()
        }
    }

    companion object {
        /** The default [bindGroupCacheSize]. */
        const val DEFAULT_BIND_GROUP_CACHE_SIZE = 256

        private const val QUAD_STATS_NAME = "SpriteBatch Quads"
        private val logger = Logger<SpriteBatch>()
    }

    private data class RenderInfo(
        val shader: Shader,
        val blendState: BlendState,
        val depthFormat: TextureFormat? = null,
        val depthTest: Boolean = true,
        val depthWrite: Boolean = false,
        val sampleCount: Int = 1,
    )

}
